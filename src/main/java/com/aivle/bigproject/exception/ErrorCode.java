package com.aivle.bigproject.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
   
    // 에러코드 목록
    // 로그인 및 회원가입
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "아이디 또는 이메일이 이미 존재합니다."),
    WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인에 실패했습니다."),
    SOCIAL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "연동된 소셜 계정을 찾을 수 없습니다."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인 후 이용가능합니다."),

    // 회사
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다."),
    COMPANY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 기업입니다."),
    COMPANY_IN_USE(HttpStatus.CONFLICT, "소속된 사용자가 있어 삭제할 수 없습니다."),

    // GitHub & 레포지토리
    GITHUB_ALREADY_CONNECTED(HttpStatus.CONFLICT, "이미 연동된 GitHub 계정입니다."),
    REPO_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 연동된 레포지토리입니다."),

    // 권한
    NO_PERMISSION(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // 토큰
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    // 서버
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");



    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
