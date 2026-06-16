package com.codemong.be.ai.dto;

import java.util.List;

public record CodeReviewResponse(
        boolean passed,
        List<String> failedTests,
        String content
) {
}
