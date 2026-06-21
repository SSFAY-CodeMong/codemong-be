package com.codemong.be.mail.dto;

import com.codemong.be.mail.entity.MailSubscription;

public record MailSubscriptionResponse(
        boolean enabled,
        String email
) {
    public static MailSubscriptionResponse from(MailSubscription subscription) {
        return new MailSubscriptionResponse(
                subscription.isEnabled(),
                subscription.getUser().getEmail()
        );
    }
}
