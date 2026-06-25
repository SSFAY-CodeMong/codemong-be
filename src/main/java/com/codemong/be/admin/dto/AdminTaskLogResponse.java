package com.codemong.be.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminTaskLogResponse(
        String id,
        String type,
        String action,
        String message,
        String status,
        Boolean success,
        Long durationMs,
        Long userId,
        Long repositoryId,
        Long branchId,
        Long step,
        String response,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
    public static AdminTaskLogResponse from(AdminTaskLogResponse response) {
        return response;
    }

    public static List<AdminTaskLogResponse> copyOf(List<AdminTaskLogResponse> logs) {
        return List.copyOf(logs);
    }
}
