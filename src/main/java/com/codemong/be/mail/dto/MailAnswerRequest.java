package com.codemong.be.mail.dto;

public record MailAnswerRequest(
        String content,
        String codeContent
) {
}
