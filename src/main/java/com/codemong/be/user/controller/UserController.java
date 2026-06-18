package com.codemong.be.user.controller;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.user.dto.UserMeResponse;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(@AuthenticationPrincipal Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
        return ResponseEntity.ok(UserMeResponse.from(user));
    }
}
