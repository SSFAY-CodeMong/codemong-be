package com.codemong.be.codecheck.dto;

import java.util.List;

public record CodeCheckStatusResponse(
        String checkId,
        Long repositoryId,
        Long step,
        String status,
        boolean passed,
        List<String> failedTests,
        String message
) {
}
