package com.aivle.bigproject.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 10, message = "이름은 10자를 초과할 수 없습니다.")
        String name,

        @NotBlank(message = "기업명은 필수입니다.")
        @Size(max = 50, message = "기업명은 50자를 초과할 수 없습니다.")
        String companyName, // 사용자는 기업'명'을 입력 → Service가 company_id(FK)로 변환

        @NotBlank(message = "Git ID는 필수입니다.")
        @Size(max = 39, message = "Git ID는 39자를 초과할 수 없습니다.")
        String gitId,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 50, message = "이메일은 50자를 초과할 수 없습니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 18, message = "비밀번호는 8자 이상 18자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
                message = "비밀번호는 영문 대문자, 소문자, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        String passwordConfirm

) {

    /**
     * password == passwordConfirm 검증
     *
     * @AssertTrue : 반환값이 true여야 통과. 메서드명이 is~ 로 시작해야 인식됨.
     * @JsonIgnore : 없으면 Jackson이 getter로 착각해 응답에 "passwordMatched" 필드를 넣음.
     */
    @JsonIgnore
    @AssertTrue(message = "비밀번호가 일치하지 않습니다.")
    public boolean isPasswordMatched() {
        if (password == null || passwordConfirm == null) {
            return true;   // null 검사는 @NotBlank가 이미 담당
        }
        return password.equals(passwordConfirm);
    }
}
