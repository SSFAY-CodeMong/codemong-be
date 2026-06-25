package com.codemong.be.user.service;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.user.dto.UserMeResponse;
import com.codemong.be.user.dto.UpdateEmailRequest;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public ResponseEntity<UserMeResponse> getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
        return ResponseEntity.ok(UserMeResponse.from(user));
    }

    @Transactional
    public void updateEmail(UpdateEmailRequest updateEmailRequest, Long userId) {
        String email = normalizeEmail(updateEmailRequest.email());
        String redisKey = userId + ":" + email + ":success";
        String value = redisTemplate.opsForValue().get(redisKey);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        if(!"success".equals(value)){
            throw new CustomException(ErrorCode.EMAIL_VERIFICATION_FAILED);
        }

        user.updateEmail(email);
        redisTemplate.delete(redisKey);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new CustomException(ErrorCode.INVALID_EMAIL);
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
