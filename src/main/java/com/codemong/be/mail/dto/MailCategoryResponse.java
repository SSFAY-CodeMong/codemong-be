package com.codemong.be.mail.dto;

import com.codemong.be.mail.entity.MailCategory;

public record MailCategoryResponse(
        Long id,
        String name
) {
    public static MailCategoryResponse from(MailCategory category) {
        return new MailCategoryResponse(category.getId(), category.getName());
    }
}
