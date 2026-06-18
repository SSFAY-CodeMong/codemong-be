package com.codemong.be.github.dto;

import com.codemong.be.repository.entity.GithubRepository;

public record RepositoryCreatedResponse(
        Long repositoryId,
        Long projectId,
        String name,
        String htmlUrl
) {
    public static RepositoryCreatedResponse from(GithubRepository repository) {
        return new RepositoryCreatedResponse(
                repository.getId(),
                repository.getProject().getId(),
                repository.getName(),
                repository.getHtmlUrl()
        );
    }
}
