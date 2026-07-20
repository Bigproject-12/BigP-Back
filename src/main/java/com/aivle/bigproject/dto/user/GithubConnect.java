package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.NotBlank;

public record GithubConnect(
    @NotBlank(message = "GitHub 토큰은 필수 입력 항목입니다.")
    String githubToken
) {
}