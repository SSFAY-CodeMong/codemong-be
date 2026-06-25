package com.codemong.be.admin.dto;

import java.time.LocalDateTime;

public record AdminRepositoryProgressResponse(
        Long repositoryId,
        String repositoryName,
        String htmlUrl,
        Long projectId,
        String projectName,
        String projectType,
        int maxStep,
        Long startStep,
        Long currentStep,
        Long branchId,
        String branchName,
        boolean currentStepPassed,
        boolean completed,
        LocalDateTime createdAt
) {
}
