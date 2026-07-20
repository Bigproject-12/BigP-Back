package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.NotBlank;

public record GithubConnect(
    @NotBlank(message = "조직명을 입력해주세요.")
    String orgName,
    String githubToken
) {
}