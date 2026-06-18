package com.codemong.be.global.oauth2.handler;

import com.codemong.be.global.jwt.JwtProvider;
import com.codemong.be.global.oauth2.user.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${codemong.frontend.redirect-uri:http://localhost:8081/}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("🎉 깃허브 로그인 성공! 토큰 발급을 시작합니다.");

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        Long userId = oAuth2User.getUserId();

        String refreshToken = jwtProvider.createRefreshToken(userId);

        String redisKey = "RT:" + userId;

        redisTemplate.opsForValue().set(
                redisKey,
                refreshToken,
                14,
                TimeUnit.DAYS
        );

        ResponseCookie rtCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/auth")
                .maxAge(60 * 60 * 24 * 14)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

        clearAuthenticationAttributes(request);
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        ResponseCookie sessionCookie = ResponseCookie.from("JSESSIONID", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());

        getRedirectStrategy().sendRedirect(request, response, frontendRedirectUri);
    }
}
