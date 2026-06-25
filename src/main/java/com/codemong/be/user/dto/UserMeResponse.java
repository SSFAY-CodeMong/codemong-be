package com.codemong.be.user.dto;

import com.codemong.be.user.entity.User;

import java.time.LocalDateTime;

public record UserMeResponse(
        Long id,
        String name,
        String email,
        String profilePath,
        String htmlUrl,
        LocalDateTime createdAt
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfilePath(),
                user.getHtmlUrl(),
                user.getCreatedAt()
        );
    }
}
