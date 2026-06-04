package com.codemong.be.github.service;

import com.codemong.be.github.dto.RepositoryDeleteResponse;
import com.codemong.be.github.dto.RepositoryInitRequest;
import com.codemong.be.user.entity.User;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import java.util.Map;

public interface GithubService {

    // test method //
    String connectTest(String param);
    GHMyself gitConnectTest();
    Map<String, GHRepository> gitRepositoryTest();
    GHRepository gitGenerateTest();
    GHRef gitGenerateBranchTest(String repoName);
    //////////////////

    Map<String, GHRepository> getRepositories(String token);
    GHRepository createProjectRepository(User user, Long projectId, RepositoryInitRequest request);
    RepositoryDeleteResponse deleteRepository(User user, Long repositoryId);

}
