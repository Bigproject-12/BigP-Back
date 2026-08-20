package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 변경 요청 DTO (로그인된 상태에서)
 * 현재 비번 -> 새비번으로 교체
 *
 * 길이/정규식/일치 여부는 여기서 @Size, @Pattern, @AssertTrue로 검증하지 않는다.
 * Bean Validation은 서비스 메서드 실행 전에 한꺼번에 평가되기 때문에,
 * "현재 비밀번호 확인 -> 새 비밀번호 검증" 순서를 보장하려면
 * UserService.changePassword에서 순서대로 직접 검증해야 한다.
 */
public record PasswordChangeRequest (
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인은 필수입니다.")
        String newPasswordConfirm

) {
}
