package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.NotBlank;

public record GithubConnect(
    @NotBlank(message = "조직명을 입력해주세요.")
    String orgName,
    @NotBlank(message = "GitHub 토큰은 필수 입력 항목입니다.")
    String githubToken
) {
}