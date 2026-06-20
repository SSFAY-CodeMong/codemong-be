package com.codemong.be.mail.dto;

public record MailEvaluationResult(
        int score,
        String feedback,
        String recommendedAnswer
) {
}
