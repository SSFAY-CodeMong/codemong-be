package com.codemong.be.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {
    private final Key key;

    private final Long ACCESS_TOKEN_VALIDITY = 1000L * 60 * 30;
    private final Long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 14;

    public JwtProvider(@Value("${jwt.secret}") String secretKey){
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String crateAccessToken(Long userId, String roleName){
        Date now = new Date();
        Date validity = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", roleName)
                .setExpiration(validity)
                .setIssuedAt(now)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId){
        Date now = new Date();
        Date validity = new Date(now.getTime() + REFRESH_TOKEN_VALIDITY);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setExpiration(validity)
                .setIssuedAt(now)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getUserId(String token){
        return Long.parseLong(
                Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject()
        );
    }

    public Date getExpiration(String token){
        return Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getExpiration();

    }

    public boolean validateToken(String token){
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        }catch (SecurityException | MalformedJwtException | UnsupportedJwtException e) {
            log.warn("🚨 유효하지 않은 JWT 토큰입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("🚨 만료된 JWT 토큰입니다.");
        }catch (IllegalArgumentException e) {
            log.warn("🚨 JWT 토큰이 비어있거나 잘못되었습니다.");
        }
        return false;
    }

    public Authentication getAuthentication(String accessToken){
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(accessToken)
                .getBody();

        Long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);

        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(role));

        return new UsernamePasswordAuthenticationToken(userId, accessToken, authorities);
    }

}
