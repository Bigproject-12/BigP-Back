package com.aivle.bigproject.dto.user;

import jakarta.validation.constraints.NotBlank;
/**
 * GitHub 조직 연동을 요청하기 위한 DTO.
 */
public record GithubConnect(
    @NotBlank(message = "조직명을 입력해주세요.")
    String orgName
) {
}