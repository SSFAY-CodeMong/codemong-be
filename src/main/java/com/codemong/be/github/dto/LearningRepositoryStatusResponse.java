package com.codemong.be.github.dto;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.process.entity.Process;
import com.codemong.be.repository.entity.GithubRepository;

import java.time.LocalDateTime;

public record LearningRepositoryStatusResponse(
        Long repositoryId,
        Long projectId,
        String projectName,
        String repositoryName,
        String htmlUrl,
        String startStep,
        String currentStep,
        String branchName,
        String branchSha,
        boolean currentStepPassed,
        boolean completed,
        LocalDateTime createdAt
) {
    public static LearningRepositoryStatusResponse from(
            GithubRepository repository,
            Process process,
            Branch branch
    ) {
        String currentStep = process == null ? null : process.getCurrentStep();
        boolean completed = branch != null && branch.isSuccess() && "step05".equals(currentStep);
        return new LearningRepositoryStatusResponse(
                repository.getId(),
                repository.getProject().getId(),
                repository.getProject().getName(),
                repository.getName(),
                repository.getHtmlUrl(),
                process == null ? null : process.getStartStep(),
                currentStep,
                branch == null ? null : branch.getName(),
                branch == null ? null : branch.getSha(),
                branch != null && branch.isSuccess(),
                completed,
                repository.getCreatedAt()
        );
    }
}
