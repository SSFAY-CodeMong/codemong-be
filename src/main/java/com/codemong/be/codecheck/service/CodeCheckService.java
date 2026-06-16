package com.codemong.be.codecheck.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.codemong.be.branch.entity.Branch;
import com.codemong.be.branch.repository.BranchRepository;
import com.codemong.be.codecheck.dto.CodeCheckCallbackRequest;
import com.codemong.be.codecheck.dto.CodeCheckResult;
import com.codemong.be.codecheck.util.CodeCheckArtifactUtil;
import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.global.kms.KmsService;
import com.codemong.be.project.entity.ProjectType;
import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.repository.repository.GithubRepositoryRepository;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CodeCheckService {

    private static final String ANSWER_REF = "main";
    private static final Duration ACTIONS_POLL_INTERVAL = Duration.ofSeconds(3);
    private static final Duration ACTIONS_RESULT_TIMEOUT = Duration.ofMinutes(3);

    private final UserRepository userRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final BranchRepository branchRepository;
    private final KmsService kmsService;
    private final ObjectMapper objectMapper;
    private final CodeCheckArtifactUtil codeCheckArtifactUtil;

    @Value("${github.answer.repository}")
    private String answerRepositoryName;

    @Value("${github.answer.token}")
    private String answerToken;

    public CodeCheckResult runGithubActionsCheck(Long repositoryId, Long step, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
        GithubRepository repository = githubRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPOSITORY_NOT_FOUND));

        if (!repository.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.REPOSITORY_NOT_FOUND);
        }

        Branch currentBranch = branchRepository.findTopByRepository_IdOrderByCreatedAtDesc(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.GITHUB_BRANCH_BASE_NOT_FOUND));

        String decryptToken = kmsService.decrypt(user.getGithubToken());

        try {
            GitHub userGitHub = new GitHubBuilder().withOAuthToken(decryptToken).build();
            GHMyself myself = userGitHub.getMyself();
            GHRepository userRepository = myself.getRepository(repository.getName());
            String targetBranchName = currentBranch.getName();
            String targetSha = userRepository.getRef("heads/" + targetBranchName).getObject().getSha();
            String projectId = repository.getProject().getName().toLowerCase();
            String stepId = currentBranch.getStep();
//            String stepId = "step01"; // 잠깐만 1
            String track = repository.getProject().getType() == ProjectType.BE ? "backend" : "frontend";
            String workflowPath = ".github/workflows/check-" + track + ".yml";
            if (!StringUtils.hasText(answerToken)) {
                throw new CustomException(ErrorCode.GITHUB_ANSWER_TOKEN_MISSING);
            }
            GitHub answerGitHub = new GitHubBuilder().withOAuthToken(answerToken).build();
            GHRepository answerRepository = answerGitHub.getRepository(answerRepositoryName);

            String fullRepositoryName = answerRepository.getFullName();
            Instant dispatchedAt = Instant.now().minusSeconds(5);

            dispatchWorkflow(
                    answerToken,
                    fullRepositoryName,
                    workflowPath,
                    myself.getLogin(),
                    repository.getName(),
                    targetSha,
                    projectId,
                    stepId
            );

            return waitWorkflowResult(answerToken, fullRepositoryName, workflowPath, dispatchedAt);
        } catch (CustomException e) {
            throw e;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.GITHUB_ACTIONS_DISPATCH_FAILED);
        }
    }

    public void receiveGithubActionsCallback(CodeCheckCallbackRequest request) {
        // TODO 1: GitHub Actions에서 온 콜백인지 서명/토큰으로 검증한다.
        // TODO 2: request.repositoryId, request.userId, request.step 기준으로 검사 요청을 찾는다.
        // TODO 3: request.passed 값을 저장한다.
        // TODO 4: AIService.codeReview가 대기/polling하는 방식이면 결과 조회 가능 상태로 변경한다.
    }


    /*
    actions 발생 트리거 호출 메소드
     */
    private void dispatchWorkflow(
            String token,
            String fullRepositoryName,
            String workflowPath,
            String userRepositoryOwner,
            String userRepositoryName,
            String targetSha,
            String projectId,
            String stepId
    ) {
        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of(
                    "ref", ANSWER_REF,
                    "inputs", Map.of(
                            "target_owner", userRepositoryOwner,
                            "target_repo", userRepositoryName,
                            "target_sha", targetSha,
                            "project_id", projectId,
                            "step_id", stepId
                    )
            ));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.GITHUB_ACTIONS_DISPATCH_FAILED);
        }

        String workflowId = URLEncoder.encode(workflowPath, StandardCharsets.UTF_8);
        URI uri = URI.create("https://api.github.com/repos/" + fullRepositoryName
                + "/actions/workflows/" + workflowId + "/dispatches");
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        for (int attempt = 1; attempt <= 5; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + token)
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new CustomException(ErrorCode.GITHUB_ACTIONS_DISPATCH_FAILED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CustomException(ErrorCode.GITHUB_ACTIONS_RESULT_TIMEOUT);
            }

            if (response.statusCode() == 204) {
                return;
            }

            if ((response.statusCode() == 404 || response.statusCode() == 422) && attempt < 5) {
                try {
                    Thread.sleep(Duration.ofSeconds(2).toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CustomException(ErrorCode.GITHUB_ACTIONS_RESULT_TIMEOUT);
                }
                continue;
            }

            if (response.statusCode() == 404) {
                throw new CustomException(ErrorCode.GITHUB_ACTIONS_WORKFLOW_NOT_FOUND);
            }
            throw new CustomException(ErrorCode.GITHUB_ACTIONS_DISPATCH_FAILED);
        }
    }

    /*
    폴링 구조로 수행 완료 긁어옴.
     */
    private CodeCheckResult waitWorkflowResult(
            String token,
            String fullRepositoryName,
            String workflowPath,
            Instant dispatchedAt
    ) {
        Instant deadline = Instant.now().plus(ACTIONS_RESULT_TIMEOUT);
        String workflowId = URLEncoder.encode(workflowPath, StandardCharsets.UTF_8);
        String branch = URLEncoder.encode(ANSWER_REF, StandardCharsets.UTF_8);
        URI uri = URI.create("https://api.github.com/repos/" + fullRepositoryName
                + "/actions/workflows/" + workflowId + "/runs?branch=" + branch
                + "&event=workflow_dispatch&per_page=10");
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        while (Instant.now().isBefore(deadline)) {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + token)
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new CustomException(ErrorCode.GITHUB_ACTIONS_RESULT_FETCH_FAILED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CustomException(ErrorCode.GITHUB_ACTIONS_RESULT_TIMEOUT);
            }

            if (response.statusCode() != 200) {
                throw new CustomException(ErrorCode.GITHUB_ACTIONS_RESULT_FETCH_FAILED);
            }

            JsonNode workflowRun = findLatestWorkflowRun(response.body(), dispatchedAt);
            if (workflowRun != null && "completed".equals(workflowRun.path("status").asText())) {
                boolean passed = "success".equals(workflowRun.path("conclusion").asText());
                if (passed) {
                    return new CodeCheckResult(true);
                }
                return new CodeCheckResult(
                        false,
                        codeCheckArtifactUtil.findFailedTests(token, fullRepositoryName, workflowRun.path("id").asText())
                );
            }

            try {
                Thread.sleep(ACTIONS_POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CustomException(ErrorCode.GITHUB_ACTIONS_RESULT_TIMEOUT);
            }
        }

        throw new CustomException(ErrorCode.GITHUB_ACTIONS_RESULT_TIMEOUT);
    }

    private JsonNode findLatestWorkflowRun(String responseBody, Instant dispatchedAt) {
        JsonNode runs;
        try {
            runs = objectMapper.readTree(responseBody).path("workflow_runs");
        } catch (IOException e) {
            throw new CustomException(ErrorCode.GITHUB_ACTIONS_RESULT_FETCH_FAILED);
        }

        if (!runs.isArray()) {
            return null;
        }

        for (JsonNode run : runs) {
            String createdAt = run.path("created_at").asText(null);
            if (createdAt == null || !Instant.parse(createdAt).isBefore(dispatchedAt)) {
                return run;
            }
        }

        return null;
    }

}
