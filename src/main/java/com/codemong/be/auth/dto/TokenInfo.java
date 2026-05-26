package com.codemong.be.auth.dto;

public record TokenInfo(
    String accessToken,
    String refreshToken
) {
}
