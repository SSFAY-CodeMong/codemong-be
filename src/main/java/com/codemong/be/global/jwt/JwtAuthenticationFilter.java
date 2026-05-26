package com.codemong.be.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = resolveToken(request);
        if(StringUtils.hasText(accessToken) && jwtProvider.validateToken(accessToken)){
            String isBlackList = redisTemplate.opsForValue().get("BL:" + accessToken);
            if(isBlackList == null){
                Authentication authentication = jwtProvider.getAuthentication(accessToken);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("정상적인 엑세스 토큰입니다. 인증을 허용합니다.");
            }else{
                log.warn("🚨 로그아웃(블랙리스트) 처리된 토큰의 접근입니다!");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "이미 로그아웃된 토큰입니다.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }
}
