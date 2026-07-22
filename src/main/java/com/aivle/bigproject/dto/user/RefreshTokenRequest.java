package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh Token을 입력해 주세요.")
        String refreshToken
) {}
