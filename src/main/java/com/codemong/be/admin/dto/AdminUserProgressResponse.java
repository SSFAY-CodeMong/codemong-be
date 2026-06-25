package com.codemong.be.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserProgressResponse(
        Long userId,
        String name,
        String email,
        String htmlUrl,
        boolean banned,
        LocalDateTime createdAt,
        List<AdminRepositoryProgressResponse> repositories
) {
}
