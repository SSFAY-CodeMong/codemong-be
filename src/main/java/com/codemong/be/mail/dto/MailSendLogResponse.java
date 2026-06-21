package com.codemong.be.mail.dto;

import com.codemong.be.mail.entity.MailSendLog;

import java.time.LocalDateTime;

public record MailSendLogResponse(
        Long id,
        String mailType,
        String recipient,
        boolean success,
        String message,
        String questionTitle,
        String contentTitle,
        LocalDateTime createdAt
) {
    public static MailSendLogResponse from(MailSendLog log) {
        return new MailSendLogResponse(
                log.getId(),
                log.getMailType(),
                log.getRecipient(),
                log.isSuccess(),
                log.getMessage(),
                log.getQuestion() == null ? null : log.getQuestion().getTitle(),
                log.getContentTitle(),
                log.getCreatedAt()
        );
    }
}
