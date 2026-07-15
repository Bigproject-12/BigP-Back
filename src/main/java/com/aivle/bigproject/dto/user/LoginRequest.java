package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "ID는 필수입니다.")
        @Email(message = "ID는 이메일 형식이어야 합니다.")
        @Size(max = 50, message = "ID는 50자를 초과할 수 없습니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password

) {
}