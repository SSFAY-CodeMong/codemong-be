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

    //User
    USER_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "메일 발송을 받으려면 이메일을 입력해야 합니다.", "40020"),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.", "40021"),
    EMAIL_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "이메일 인증에 실패하였습니다.", "40022"),
    EMAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "인증 메일 발송에 실패하였습니다.", "50220"),

    //Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다.", "40401"),
    PROJECT_SPEC_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트 요구사항을 찾을 수 없습니다.", "40403"),

    //Repository
    REPOSITORY_NOT_FOUND(HttpStatus.NOT_FOUND, "레포지토리를 찾을 수 없습니다.", "40402"),
    INVALID_REPOSITORY_REQUEST(HttpStatus.BAD_REQUEST, "레포지토리 요청 값이 올바르지 않습니다.", "40010"),
    INVALID_REPOSITORY_NAME(HttpStatus.BAD_REQUEST, "프로젝트 이름으로 레포지토리명을 만들 수 없습니다.", "40011"),
    REPOSITORY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "레포지토리에 접근할 권한이 없습니다.", "40302"),
    BRANCH_NOT_SUCCESS(HttpStatus.CONFLICT, "현재 브랜치 검사를 통과해야 다음 단계로 넘어갈 수 있습니다.", "40901"),
    NEXT_STEP_NOT_FOUND(HttpStatus.BAD_REQUEST, "다음 단계가 존재하지 않습니다.", "40012"),

    //GitHub
    GITHUB_REPOSITORY_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "GitHub 레포지토리 조회에 실패하였습니다.", "50201"),
    GITHUB_REPOSITORY_CREATE_FAILED(HttpStatus.BAD_GATEWAY, "GitHub 레포지토리 생성에 실패하였습니다.", "50202"),
    GITHUB_REPOSITORY_DELETE_FAILED(HttpStatus.BAD_GATEWAY, "GitHub 레포지토리 삭제에 실패하였습니다.", "50203"),
    GITHUB_BRANCH_CREATE_FAILED(HttpStatus.BAD_GATEWAY, "GitHub 브랜치 생성에 실패하였습니다.", "50204"),
    GITHUB_BRANCH_BASE_NOT_FOUND(HttpStatus.BAD_GATEWAY, "브랜치를 생성할 기준 브랜치를 찾을 수 없습니다.", "50205"),
    GITHUB_ANSWER_REPOSITORY_NOT_FOUND(HttpStatus.BAD_GATEWAY, "정답 레포지토리에 접근할 수 없습니다.", "50206"),
    GITHUB_ANSWER_TOKEN_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "정답 레포지토리 토큰 설정이 필요합니다.", "50056"),
    GITHUB_ANSWER_CODE_COPY_FAILED(HttpStatus.BAD_GATEWAY, "정답 코드 복사에 실패하였습니다.", "50207"),
    GITHUB_ACTIONS_WORKFLOW_NOT_FOUND(HttpStatus.BAD_GATEWAY, "GitHub Actions workflow를 찾을 수 없습니다.", "50208"),
    GITHUB_ACTIONS_DISPATCH_FAILED(HttpStatus.BAD_GATEWAY, "GitHub Actions 실행 요청에 실패하였습니다.", "50209"),
    GITHUB_ACTIONS_RESULT_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "GitHub Actions 결과 대기 시간이 초과되었습니다.", "50401"),
    GITHUB_ACTIONS_RESULT_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "GitHub Actions 결과 조회에 실패하였습니다.", "50210"),
    GITHUB_ANSWER_REPOSITORY_WRITE_DENIED(HttpStatus.FORBIDDEN, "정답 레포지토리 쓰기 권한이 없습니다.", "40301");

    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
