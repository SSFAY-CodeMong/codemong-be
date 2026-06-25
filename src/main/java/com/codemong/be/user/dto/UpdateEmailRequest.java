package com.codemong.be.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record UpdateEmailRequest(
        @Email
        @NotBlank String email
) {
}
