package com.codemong.be.report.dto;

public record BranchFeedback(
        Long step,
        String branchName,
        String content
) {
}
