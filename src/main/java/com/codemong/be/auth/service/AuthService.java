package com.codemong.be.auth.service;

import com.codemong.be.auth.dto.TokenInfo;
import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.global.jwt.JwtProvider;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    public TokenInfo reissue(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.MISSING_TOKEN);
        }

        Long userId = extractUserId(refreshToken);
        String redisKey = "RT:" + userId;

        String redisRefreshToken = redisTemplate.opsForValue().get(redisKey);
        if(!refreshToken.equals(redisRefreshToken)){
            redisTemplate.delete(redisKey);
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        String newRefreshToken = jwtProvider.createRefreshToken(userId);
        String newAccessToken = jwtProvider.crateAccessToken(userId, user.getRole().getName());
        redisTemplate.opsForValue().set(
                redisKey,
                newRefreshToken,
                14,
                TimeUnit.DAYS
        );

        log.debug("----------reissue------------");
        return new TokenInfo(newAccessToken, newRefreshToken);
    }

    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new CustomException(ErrorCode.MISSING_TOKEN);
        }

        Long userId = extractUserId(accessToken);
        String redisKey = "BL:" + accessToken;
        Date expiration = extractExpiration(accessToken);
        Long remainingTime = expiration.getTime() - System.currentTimeMillis();
        redisTemplate.opsForValue().set(
                redisKey,
                "logout",
                remainingTime,
                TimeUnit.MILLISECONDS
        );

        redisTemplate.delete("RT:" + userId);
        SecurityContextHolder.clearContext();
    }

    private Long extractUserId(String token) {
        try {
            return jwtProvider.getUserId(token);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.MISSING_TOKEN);
        }
    }

    private Date extractExpiration(String token) {
        try {
            return jwtProvider.getExpiration(token);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.MISSING_TOKEN);
        }
    }
}
