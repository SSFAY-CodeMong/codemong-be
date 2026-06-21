package com.codemong.be.mail.dto;

import com.codemong.be.mail.entity.MailAnswer;

import java.time.LocalDateTime;

public record MailAnswerResponse(
        Long id,
        Long questionId,
        String questionTitle,
        String content,
        String codeContent,
        int score,
        String feedback,
        String recommendedAnswer,
        LocalDateTime createdAt
) {
    public static MailAnswerResponse from(MailAnswer answer) {
        return new MailAnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getQuestion().getTitle(),
                answer.getContent(),
                answer.getCodeContent(),
                answer.getScore(),
                answer.getFeedback(),
                answer.getRecommendedAnswer(),
                answer.getCreatedAt()
        );
    }
}
