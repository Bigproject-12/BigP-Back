package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.NotBlank;
/**
 * Access Token 재발급을 위한 Refresh Token 요청 DTO.
 *
 * 기존 Access Token이 만료되었을 때 클라이언트로부터
 * Refresh Token을 전달받아 새로운 Access Token을 발급하는 데 사용한다.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh Token을 입력해 주세요.")
        String refreshToken
) {}
