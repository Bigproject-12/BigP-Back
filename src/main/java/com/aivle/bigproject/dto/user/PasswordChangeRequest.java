package com.aivle.bigproject.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청 DTO (로그인된 상태에서)
 * 현재 비번 -> 새비번으로 교체
 */

public record PasswordChangeRequest (
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, max = 30, message = "비밀번호는 8자 이상 30자 이하여야 합니다.")
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인은 필수입니다.")
        String newPasswordConfirm

) {
    /**
     * newPassword = newPasswordConfirm  검증
     */

    @JsonIgnore
    @AssertTrue(message = "새 비밀번호가 일치하지 않습니다.")
    public boolean isNewPasswordMatched() {
        if (newPassword == null || newPasswordConfirm == null) {
            return true;
        }
        return newPassword.equals(newPasswordConfirm);
    }
}
