package com.codemong.be.mail.dto;

import java.util.List;

public record MailDashboardResponse(
        MailSubscriptionResponse subscription,
        MailQuestionResponse previewQuestion,
        MailContentResponse previewContent,
        List<MailSendLogResponse> recentLogs
) {
}
