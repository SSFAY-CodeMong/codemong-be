package com.codemong.be.user.dto;

import com.codemong.be.user.entity.User;

public record UserMeResponse(
        Long id,
        String name,
        String email,
        String profilePath,
        String htmlUrl
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfilePath(),
                user.getHtmlUrl()
        );
    }
}
