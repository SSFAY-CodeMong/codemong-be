package com.codemong.be.github.service.impl;

import com.codemong.be.github.service.GithubService;
import com.codemong.be.global.kms.KmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GithubServiceImpl implements GithubService {

    @Value("${github.token}")
    private String token;

    private final KmsService kmsService;


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
    public GHRepository createProjectRepository(String token, String projectName) {
        try {
            String decryptToken = kmsService.decrypt(token);
            GitHub gitHub = new GitHubBuilder().withOAuthToken(decryptToken).build();
            Map<String, GHRepository> repositories = gitHub.getMyself().getRepositories();

            String repositoryNamePrefix = "codemong-" + normalizeRepositoryName(projectName);
            int repoNum = 0;
            String repositoryName = repositoryNamePrefix + "-" + repoNum;
            while (repositories.containsKey(repositoryName)) {
                repoNum++;
                repositoryName = repositoryNamePrefix + "-" + repoNum;
            }

            log.info("Creating GitHub repository: {}", repositoryName);

            return gitHub.createRepository(repositoryName)
                    .description("Codemong project repository")
                    .private_(true)
                    .autoInit(true)
                    .create();
        } catch (IOException e) {
            log.error("Failed to create GitHub project repository", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteProjectRepository(String token, String repositoryName) {
        try {
            String decryptToken = kmsService.decrypt(token);
            GitHub gitHub = new GitHubBuilder().withOAuthToken(decryptToken).build();
            String login = gitHub.getMyself().getLogin();
            GHRepository repository = gitHub.getRepository(login + "/" + repositoryName);
            repository.delete();
        } catch (IOException e) {
            log.error("Failed to delete GitHub project repository: {}", repositoryName, e);
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
}
