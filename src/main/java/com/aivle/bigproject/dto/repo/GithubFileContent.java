package com.aivle.bigproject.dto.repo;

/**
 * GitHub 파일 내용을 반환하기 위한 응답 DTO.
 *
 * 파일의 내용과 SHA 값을 포함
 */
public record GithubFileContent(
        String content,
        String sha
) {}
