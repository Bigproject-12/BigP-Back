package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 재설정 코드 요청 DTO (로그인 안된 상태에서)
 * 사용자가 비밀번호 재설정을 위해 이메일을 입력하면, 서버는 해당 이메일로 재설정 코드를 발송
 */
public record PasswordResetCodeRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String loginId
) {
}
