package com.codemong.be.admin.dto;

public record AdminMetricsResponse(
        long users,
        long repositories,
        long runningTasks,
        long completedTasks,
        long successfulTasks,
        long failedTasks,
        long usedMemoryMb,
        long maxMemoryMb
) {
}
