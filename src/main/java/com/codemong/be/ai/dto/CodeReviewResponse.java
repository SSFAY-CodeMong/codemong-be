package com.codemong.be.ai.dto;

import java.util.List;

public record CodeReviewResponse(
        boolean passed,
        List<String> failedTests,
        List<FailedTestResponse> failedTestDetails,
        String content,
        boolean isSaved
) {
}
