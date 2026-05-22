package com.codemong.be.github.service.impl;

import com.codemong.be.github.service.GithubService;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class GithubServiceImpl implements GithubService {

    @Value("${github.token}")
    private String token;

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
            if(branches.containsKey(branchName)) throw new RuntimeException();


            // 3. 브랜치 생성

            GHRef ref = repo.getRef("heads/main");
            String sha = ref.getObject().getSha();

            return repo.createRef("refs/heads/"+branchName, sha);

        } catch (IOException e) {
            log.error("Failed to create GitHub repository", e);
            throw new RuntimeException(e);
        }
    }
}
