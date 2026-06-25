package com.codemong.be.admin.service;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {

    private static final String ADMIN_USERNAME = "ysbchc0001";
    private static final String ADMIN_PASSWORD = "admin8888";

    private final Set<String> activeTokens = ConcurrentHashMap.newKeySet();

    public String login(String username, String password) {
        if (!ADMIN_USERNAME.equals(username) || !ADMIN_PASSWORD.equals(password)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        String token = UUID.randomUUID().toString();
        activeTokens.add(token);
        return token;
    }

    public void validate(String token) {
        if (!StringUtils.hasText(token) || !activeTokens.contains(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
}
