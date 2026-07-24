package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetVerifyRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        String loginId,

        @NotBlank(message = "인증 코드는 필수입니다.")
        String code
) {
}
