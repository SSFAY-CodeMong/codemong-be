package com.codemong.be.mail.dto;

import com.codemong.be.mail.entity.MailQuestion;

public record MailQuestionResponse(
        Long id,
        String category,
        String title,
        String content,
        String codeTemplate,
        String difficulty,
        String questionType
) {
    public static MailQuestionResponse from(MailQuestion question) {
        return new MailQuestionResponse(
                question.getId(),
                question.getCategory().getName(),
                question.getTitle(),
                question.getContent(),
                question.getCodeTemplate(),
                question.getDifficulty().name(),
                question.getQuestionType().name()
        );
    }
}
