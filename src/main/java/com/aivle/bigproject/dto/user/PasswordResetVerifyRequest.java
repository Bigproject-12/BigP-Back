package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.NotBlank;

/**
 *  비밀번호 재설정을 위한 인증 코드 검증 요청 DTO.
 */
public record PasswordResetVerifyRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        String loginId,

        @NotBlank(message = "인증 코드는 필수입니다.")
        String code
) {
}
