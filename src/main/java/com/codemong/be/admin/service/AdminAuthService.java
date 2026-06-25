package com.codemong.be.admin.service;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {

    private final Set<String> activeTokens = ConcurrentHashMap.newKeySet();

    @Value("${codemong.admin.username:CMADMIN}")
    private String adminUsername;

    @Value("${codemong.admin.password:PUHvWbokMjY010TinXkPzYlj}")
    private String adminPassword;

    public String login(String username, String password) {
        if (!adminUsername.equals(username) || !adminPassword.equals(password)) {
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
