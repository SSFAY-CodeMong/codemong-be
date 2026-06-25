package com.codemong.be.user.controller;

import com.codemong.be.user.dto.UserMeResponse;
import com.codemong.be.user.dto.UpdateEmailRequest;
import com.codemong.be.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(@AuthenticationPrincipal Long userId) {
        return userService.getMyInfo(userId);
    }

    @PatchMapping
    public ResponseEntity<Void> updateEmail(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateEmailRequest updateEmailRequest
    ){

        userService.updateEmail(updateEmailRequest, userId);
        return ResponseEntity.noContent().build();
    }


}
