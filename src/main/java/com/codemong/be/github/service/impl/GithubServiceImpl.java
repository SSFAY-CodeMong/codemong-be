package com.codemong.be.github.service.impl;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.branch.repository.BranchRepository;
import com.codemong.be.github.dto.RepositoryInitRequest;
import com.codemong.be.github.dto.RepositoryDeleteResponse;
import com.codemong.be.github.service.GithubService;
import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.global.kms.KmsService;
import com.codemong.be.process.entity.Process;
import com.codemong.be.process.repository.ProcessRepository;
import com.codemong.be.project.entity.Project;
import com.codemong.be.project.entity.ProjectType;
import com.codemong.be.project.repository.ProjectRepository;
import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.repository.repository.GithubRepositoryRepository;
import com.codemong.be.setup.entity.Setup;
import com.codemong.be.setup.repository.SetupRepository;
import com.codemong.be.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GithubServiceImpl implements GithubService {

    @Value("${github.token}")
    private String token;

    @Value("${github.answer.token}")
    private String answerToken;

    @Value("${github.answer.repository}")
    private String answerRepositoryName;

    private final KmsService kmsService;
    private final ProjectRepository projectRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final SetupRepository setupRepository;
    private final ProcessRepository processRepository;
    private final BranchRepository branchRepository;


    @Override
    public Map<String, GHRepository> getRepositories(String token) {
        try {
            String decryptToken = kmsService.decrypt(token);
            GitHub gitHub = new GitHubBuilder().withOAuthToken(decryptToken).build();
            return gitHub.getMyself().getRepositories();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GHRepository createProjectRepository(User user, Long projectId, RepositoryInitRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
        validateRepositoryInitRequest(request);

        try {
            String decryptToken = kmsService.decrypt(user.getGithubToken());
            GitHub gitHub = new GitHubBuilder().withOAuthToken(decryptToken).build();
            Map<String, GHRepository> repositories = gitHub.getMyself().getRepositories();

            String repositoryNamePrefix = "codemong-" + normalizeRepositoryName(project.getName());
            int repoNum = 0;
            String repositoryName = repositoryNamePrefix + "-" + repoNum;
            while (repositories.containsKey(repositoryName)) {
                repoNum++;
                repositoryName = repositoryNamePrefix + "-" + repoNum;
            }

            log.info("Creating GitHub repository: {}", repositoryName);

            GHRepository createdRepository = gitHub.createRepository(repositoryName)
                    .description("Codemong project repository")
                    .private_(true)
                    .autoInit(true)
                    .create();
            GithubRepository savedRepository = githubRepositoryRepository.save(new GithubRepository(
                    user,
                    project,
                    createdRepository.getName(),
                    createdRepository.getHtmlUrl().toString()
            ));

            initializeRepositoryStep(user, project, savedRepository, createdRepository, request);

            return createdRepository;
        } catch (IOException e) {
            log.error("Failed to create GitHub project repository", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public RepositoryDeleteResponse deleteRepository(User user, Long repositoryId) {
        GithubRepository repository = githubRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPOSITORY_NOT_FOUND));
        if (!repository.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.REPOSITORY_NOT_FOUND);
        }

        try {
            String decryptToken = kmsService.decrypt(user.getGithubToken());
            GitHub gitHub = new GitHubBuilder().withOAuthToken(decryptToken).build();
            String login = gitHub.getMyself().getLogin();
            GHRepository remoteRepository = gitHub.getRepository(login + "/" + repository.getName());
            remoteRepository.delete();

            RepositoryDeleteResponse response = new RepositoryDeleteResponse(
                    repository.getId(),
                    repository.getName(),
                    repository.getHtmlUrl()
            );
            githubRepositoryRepository.delete(repository);

            return response;
        } catch (IOException e) {
            log.error("Failed to delete GitHub project repository: {}", repository.getName(), e);
            throw new RuntimeException(e);
        }
    }

    private String normalizeRepositoryName(String projectName) {
        String normalized = projectName.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("프로젝트 이름으로 레포지토리명을 만들 수 없습니다.");
        }

        return normalized;
    }

    private void initializeRepositoryStep(
            User user,
            Project project,
            GithubRepository savedRepository,
            GHRepository createdRepository,
            RepositoryInitRequest request
    ) throws IOException {
        String startStep = formatStep(request.getStartStep());
        String frontendAnswerStep = formatStep(request.getStartStep());
        String backendAnswerStep = formatStep(resolveBackendAnswerStep(request.getStartStep()));
        String branchName = buildBranchName(project.getName(), startStep);

        log.info(
                "Initializing repository. projectId={}, startStep={}, frontendAnswerStep={}, backendAnswerStep={}, targetBranch={}",
                project.getId(),
                startStep,
                frontendAnswerStep,
                backendAnswerStep,
                branchName
        );

        GHRef createdBranch = createBranch(createdRepository, branchName);
        copyAnswerRepositoryContents(createdRepository, branchName, frontendAnswerStep, ProjectType.FE);
        copyAnswerRepositoryContents(createdRepository, branchName, backendAnswerStep, ProjectType.BE);

        String finalBranchSha = createdRepository.getRef("heads/" + branchName).getObject().getSha();
        String frontendAnswerSha = getAnswerBranchSha(frontendAnswerStep);
        String backendAnswerSha = getAnswerBranchSha(backendAnswerStep);

        setupRepository.save(new Setup(project, frontendAnswerStep, ProjectType.FE, frontendAnswerSha));
        setupRepository.save(new Setup(project, backendAnswerStep, ProjectType.BE, backendAnswerSha));
        processRepository.save(new Process(user, savedRepository, startStep, startStep));
        branchRepository.save(new Branch(
                user,
                savedRepository,
                branchName,
                startStep,
                finalBranchSha == null ? createdBranch.getObject().getSha() : finalBranchSha
        ));
    }

    private GHRef createBranch(GHRepository repository, String branchName) throws IOException {
        String baseBranchName = repository.getDefaultBranch();
        GHRef baseRef = repository.getRef("heads/" + baseBranchName);
        String baseSha = baseRef.getObject().getSha();
        return repository.createRef("refs/heads/" + branchName, baseSha);
    }

    private void copyAnswerRepositoryContents(
            GHRepository targetRepository,
            String targetBranchName,
            String answerStep,
            ProjectType type
    ) throws IOException {
        validateAnswerRepositoryToken();
        GitHub answerGitHub = new GitHubBuilder().withOAuthToken(answerToken).build();
        GHRepository answerRepository = getAnswerRepository(answerGitHub);
        log.info(
                "Copying answer contents. sourceRepository={}, sourceBranch={}, sourcePath={}, targetRepository={}, targetBranch={}",
                answerRepositoryName,
                answerStep,
                type.name(),
                targetRepository.getFullName(),
                targetBranchName
        );
        copyDirectory(answerRepository, targetRepository, type.name(), answerStep, targetBranchName);
    }

    private void copyDirectory(
            GHRepository sourceRepository,
            GHRepository targetRepository,
            String path,
            String sourceBranchName,
            String targetBranchName
    ) throws IOException {
        List<GHContent> contents = sourceRepository.getDirectoryContent(path, sourceBranchName);
        for (GHContent content : contents) {
            if (content.isDirectory()) {
                copyDirectory(sourceRepository, targetRepository, content.getPath(), sourceBranchName, targetBranchName);
                continue;
            }

            if (content.isFile()) {
                upsertContent(targetRepository, targetBranchName, content.getPath(), content.read().readAllBytes());
            }
        }
    }

    private void upsertContent(GHRepository repository, String branchName, String path, byte[] content) throws IOException {
        try {
            GHContent existingContent = repository.getFileContent(path, branchName);
            existingContent.update(content, "Initialize answer code: " + path, branchName);
        } catch (GHFileNotFoundException e) {
            repository.createContent()
                    .branch(branchName)
                    .path(path)
                    .content(content)
                    .message("Initialize answer code: " + path)
                    .commit();
        }
    }

    private String getAnswerBranchSha(String answerStep) throws IOException {
        validateAnswerRepositoryToken();
        GitHub answerGitHub = new GitHubBuilder().withOAuthToken(answerToken).build();
        GHRepository answerRepository = getAnswerRepository(answerGitHub);
        return answerRepository.getRef("heads/" + answerStep).getObject().getSha();
    }

    private GHRepository getAnswerRepository(GitHub answerGitHub) throws IOException {
        try {
            return answerGitHub.getRepository(answerRepositoryName);
        } catch (GHFileNotFoundException e) {
            throw new IllegalStateException(
                    "정답 레포지토리에 접근할 수 없습니다. GITHUB_ANSWER_REPOSITORY=" + answerRepositoryName
                            + ", GITHUB_ANSWER_TOKEN 권한을 확인하세요.",
                    e
            );
        }
    }

    private long resolveBackendAnswerStep(Long startStep) {
        return Math.max(1, startStep - 1);
    }

    private String formatStep(Long step) {
        return String.format("step%02d", step);
    }

    private String buildBranchName(String projectName, String step) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        return normalizeRepositoryName(projectName) + "-" + step + "-" + date;
    }

    private void validateRepositoryInitRequest(RepositoryInitRequest request) {
        if (request == null || request.getStartStep() == null) {
            throw new IllegalArgumentException("startStep은 필수입니다.");
        }

        if (request.getStartStep() < 1 || request.getStartStep() > 5) {
            throw new IllegalArgumentException("startStep은 1부터 5까지 가능합니다.");
        }
    }

    private void validateAnswerRepositoryToken() {
        if (!StringUtils.hasText(answerToken)) {
            throw new IllegalStateException("GITHUB_ANSWER_TOKEN 설정이 필요합니다.");
        }
    }


    private String findBaseBranchName(GHRepository repo, Map<String, GHBranch> branches) {
        String defaultBranch = repo.getDefaultBranch();
        if (defaultBranch != null && branches.containsKey(defaultBranch)) {
            return defaultBranch;
        }

        if (branches.containsKey("main")) {
            return "main";
        }

        if (branches.containsKey("master")) {
            return "master";
        }

        throw new IllegalStateException("브랜치를 생성할 기준 브랜치가 없습니다.");
    }

////////////////// TEST METHODS ///////////////////
    @Override
    public String connectTest(String param) {
        return param;
    }

    @Override
    public GHMyself gitConnectTest() {
        try {
            GitHub github = new GitHubBuilder()
                    .withOAuthToken(token)
                    .build();
            return github.getMyself();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, GHRepository> gitRepositoryTest() {
        try {
            GitHub gitHub = new GitHubBuilder().withOAuthToken(token).build();
            return gitHub.getMyself().getRepositories();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GHRepository gitGenerateTest() {
        try {
            GitHub client = new GitHubBuilder().withOAuthToken(token).build();
            Map<String, GHRepository> repositories = client.getMyself().getRepositories();

            int repoNum = 0;
            String repoName = "test-repo" + repoNum;
            while (repositories.containsKey(repoName)) {
                repoNum++;
                repoName = "test-repo" + repoNum;
            }

            log.info("Creating GitHub repository: {}", repoName);

            return client
                    .createRepository(repoName)
                    .description("API Created")
                    .private_(true)
                    .autoInit(true)
                    .create();
        } catch (IOException e) {
            log.error("Failed to create GitHub repository", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public GHRef gitGenerateBranchTest(String repoName) {
        try {
            GitHub client = new GitHubBuilder().withOAuthToken(token).build();

            // 1. repo 유효성 검사
            GHRepository repo = client.getRepository(repoName);
            // 2. branch명 검사
            String branchName = "board-step01-260522";
            // 브랜치는 board-step01-260522 형식이므로 process 테이블조회가 필요함

            Map<String, GHBranch> branches = repo.getBranches();
            if (branches.containsKey(branchName)) {
                throw new IllegalStateException("이미 존재하는 브랜치입니다: " + branchName);
            }


            // 3. 브랜치 생성

            String baseBranchName = findBaseBranchName(repo, branches);
            GHRef ref = repo.getRef("heads/" + baseBranchName);
            String sha = ref.getObject().getSha();

            return repo.createRef("refs/heads/" + branchName, sha);

        } catch (IOException e) {
            log.error("Failed to create GitHub branch", e);
            throw new RuntimeException(e);
        }
    }

}
