package com.codemong.be.mail.dto;

import com.codemong.be.mail.entity.MailContent;

public record MailContentResponse(
        String id,
        String type,
        String track,
        String sourceFile,
        int displayOrder,
        String title,
        String content
) {
    public static MailContentResponse from(MailContent content) {
        return new MailContentResponse(
                content.getTrack() + "/" + content.getSourceFile().replaceFirst("\\.md$", ""),
                content.getType().name(),
                content.getTrack(),
                content.getSourceFile(),
                content.getDisplayOrder(),
                content.getTitle(),
                content.getContent()
        );
    }
}
