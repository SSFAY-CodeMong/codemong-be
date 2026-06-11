package com.codemong.be.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record UserQuestionRequest(
        @NotBlank String question
) {
}
