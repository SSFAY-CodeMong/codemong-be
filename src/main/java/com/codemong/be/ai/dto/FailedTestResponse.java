package com.codemong.be.ai.dto;

public record FailedTestResponse(
        String name,
        String methodName,
        String description
) {
}
