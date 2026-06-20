package com.codemong.be.report.dto;

import com.codemong.be.report.entity.Report;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        Long repositoryId,
        String content,
        int score,
        LocalDateTime createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getGithubRepository().getId(),
                report.getContent(),
                report.getScore(),
                report.getCreatedAt()
        );
    }
}
