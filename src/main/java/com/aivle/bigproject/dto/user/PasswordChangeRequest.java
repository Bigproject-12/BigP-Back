package com.aivle.bigproject.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청 DTO (로그인된 상태에서)
 * 현재 비번 -> 새비번으로 교체
 */

public record PasswordChangeRequest (
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, max = 18, message = "비밀번호는 8자 이상 18자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
                message = "비밀번호는 영문 대문자, 소문자, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다."
        )
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
