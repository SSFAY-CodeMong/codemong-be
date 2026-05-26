package com.codemong.be.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
     //Token
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", "40101"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다.", "40102"),
    MISSING_TOKEN(HttpStatus.BAD_REQUEST, "토큰이 존재하지 않습니다.", "40001"),
    INVALID_AUTH_HEADER(HttpStatus.BAD_REQUEST, "Authorization 헤더 형식이 올바르지 않습니다.", "40002"),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "로그아웃된 토큰입니다.", "40103"),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "저장된 리프레시 토큰과 일치하지 않습니다.", "40104"),

    //KMS
    KMS_ACCESS_DENIED(HttpStatus.INTERNAL_SERVER_ERROR, "KMS 접근 권한이 없습니다.", "50051"),
    KMS_KEY_UNAVAILABLE(HttpStatus.INTERNAL_SERVER_ERROR, "사용할 수 없는 KMS 키입니다.", "50052"),
    KMS_ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 암호화에 실패하였습니다.", "50053"),
    KMS_DECRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 복호화에 실패하였습니다.", "50054"),

    //OAuth
    OAUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "OAuth 로그인 처리에 실패하였습니다.", "40105"),

    //Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다.", "40401");

    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
