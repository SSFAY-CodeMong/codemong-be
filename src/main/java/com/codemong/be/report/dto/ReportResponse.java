package com.codemong.be.report.dto;

import com.codemong.be.feedback.dto.FeedbackDetail;

import java.time.LocalDateTime;
import java.util.List;

public record ReportResponse(
        Long id,
        Long repositoryId,
        String projectName,
        String content,
        int score,
        LocalDateTime createdAt,
        List<FeedbackDetail> feedbackDetails
) {
}
