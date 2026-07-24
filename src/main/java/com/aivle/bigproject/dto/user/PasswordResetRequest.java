package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 인증 코드로 비밀번호를 재설정하는 요청.
 * 길이/정규식/일치 여부는 PasswordChangeRequest와 같은 이유로 여기서 검증하지 않고
 * UserService.resetPassword에서 순서대로 직접 검증한다.
 */
public record PasswordResetRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        String loginId,

        @NotBlank(message = "인증 코드는 필수입니다.")
        String code,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인은 필수입니다.")
        String newPasswordConfirm
) {
}
