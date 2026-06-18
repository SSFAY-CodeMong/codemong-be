package com.codemong.be.github.dto;

import com.codemong.be.repository.entity.GithubRepository;

import java.time.LocalDateTime;

public record LearningRepositoryResponse(
        Long repositoryId,
        Long projectId,
        String projectName,
        String projectType,
        String name,
        String htmlUrl,
        LocalDateTime createdAt
) {
    public static LearningRepositoryResponse from(GithubRepository repository) {
        return new LearningRepositoryResponse(
                repository.getId(),
                repository.getProject().getId(),
                repository.getProject().getName(),
                repository.getProject().getType().name(),
                repository.getName(),
                repository.getHtmlUrl(),
                repository.getCreatedAt()
        );
    }
}
