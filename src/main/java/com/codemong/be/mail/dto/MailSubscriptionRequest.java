package com.codemong.be.mail.dto;

public record MailSubscriptionRequest(
        boolean enabled,
        String email
) {
}
