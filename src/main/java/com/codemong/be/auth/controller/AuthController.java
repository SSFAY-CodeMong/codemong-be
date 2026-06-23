package com.codemong.be.auth.controller;

import com.codemong.be.auth.dto.ReissueResponse;
import com.codemong.be.auth.dto.TokenInfo;
import com.codemong.be.auth.service.AuthService;
import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(
            @CookieValue(name = "refresh_token", required = false) String refreshToken
    ){
        if(refreshToken == null){
            throw new CustomException(ErrorCode.MISSING_TOKEN);
        }

        TokenInfo tokenInfo = authService.reissue(refreshToken);
        String newAccessToken = tokenInfo.accessToken();
        String newRefreshToken = tokenInfo.refreshToken();

        ResponseCookie responseCookie = ResponseCookie.from("refresh_token", newRefreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/auth")
                .maxAge(60 * 60 * 24 * 14)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(new ReissueResponse(newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String breakingToken,
            HttpServletRequest request
    ){
        if (breakingToken == null || breakingToken.isBlank()) {
            throw new CustomException(ErrorCode.MISSING_TOKEN);
        }

        String accessToken = null;
        if(breakingToken.startsWith("Bearer ")){
            accessToken = breakingToken.substring(7);
        } else {
            throw new CustomException(ErrorCode.INVALID_AUTH_HEADER);
        }
        authService.logout(accessToken);

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .maxAge(0)
                .httpOnly(true)
                .secure(false)
                .path("/auth")
                .sameSite("Lax")
                .build();

        ResponseCookie sessionCookie = ResponseCookie.from("JSESSIONID", "")
                .maxAge(0)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .build();
    }
}
