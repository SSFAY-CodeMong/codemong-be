package com.codemong.be.mail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record VerifyCodeRequest(
        @Email
        @NotBlank String email,
        @NotBlank String code
) {
}
