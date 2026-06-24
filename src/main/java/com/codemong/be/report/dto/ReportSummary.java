package com.codemong.be.report.dto;

import java.time.LocalDateTime;

public record ReportSummary(
        Long id,
        Long repositoryId,
        String projectName,
        String content,
        int score,
        LocalDateTime createdAt
) {
}
