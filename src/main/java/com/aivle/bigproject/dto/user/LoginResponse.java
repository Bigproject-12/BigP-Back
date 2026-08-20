package com.aivle.bigproject.dto.user;

/**
 * 로그인 성공 응답 DTO
 * 로그인 성공시 -> 토근 + 최소한의 유저 정보 반환
 * 하지만 인증 방식(JWT) 미확정으로 accessToken / tokenType은 자리만 잡음
 * 실제 토큰 발급 로직은 인증 기능 구현 시 service
 */

public record LoginResponse (
    String accessToken, // 로그인 후 인증에 쓸 토큰

    String refreshToken,

    String tokenType, // 토큰 인증 방식 표기
    Integer userId,
    String name,
    String role,
    String githubToken
){
    /**
     * 토큰과 유저 정보로 로그인 응답 생성
     * tokentype은 우선 "bearer"로 고정
     */
    public static LoginResponse of(
            String accessToken,
            String refreshToken,
            Integer userId,
            String name,
            String role,
            String githubToken
    ) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", userId, name, role, githubToken);
    }
}
