package com.codemong.be.codecheck.dto;

public record CodeCheckCallbackRequest(
        Long repositoryId,
        Long userId,
        Long step,
        String branchName,
        String workflowRunId,
        Boolean passed
) {
}
