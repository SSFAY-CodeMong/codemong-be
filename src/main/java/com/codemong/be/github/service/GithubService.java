package com.codemong.be.github.service;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.github.dto.RepositoryDeleteResponse;
import com.codemong.be.github.dto.RepositoryInitRequest;
import com.codemong.be.user.entity.User;
import org.kohsuke.github.GHRepository;

import java.util.Map;

public interface GithubService {

    Map<String, GHRepository> getRepositories(String token);
    GHRepository createProjectRepository(User user, Long projectId, RepositoryInitRequest request);
    RepositoryDeleteResponse deleteRepository(User user, Long repositoryId);

    Map<String, String> getBranchContents(Long repositoryId, Long step, Long userId);


    Boolean validateRepoOwner(Long repositoryId, Long userId);
    Boolean validateIsSuccess(Long repositoryId);
    Branch createNextStepBranch(Long repositoryId, Long userId);
}
