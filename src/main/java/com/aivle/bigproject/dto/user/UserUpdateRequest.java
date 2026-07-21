package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 회원정보 부분 수정 요청. null인 필드는 기존 값을 유지한다. */
public record UserUpdateRequest(
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        @Pattern(regexp = ".*\\S.*", message = "이름은 공백일 수 없습니다.")
        String name,

        @Size(max = 100, message = "회사명은 100자 이하여야 합니다.")
        @Pattern(regexp = ".*\\S.*", message = "회사명은 공백일 수 없습니다.")
        String companyName
) {
}
