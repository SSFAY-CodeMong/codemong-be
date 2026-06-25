package com.codemong.be.admin.dto;

public record AdminLoginRequest(
        String username,
        String password
) {
}
