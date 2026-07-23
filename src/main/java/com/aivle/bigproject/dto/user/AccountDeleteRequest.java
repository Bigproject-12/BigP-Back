package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.NotBlank;

/** 회원 탈퇴 전 현재 비밀번호를 다시 확인한다. */
public record AccountDeleteRequest(
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword
) {
}
