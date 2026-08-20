package com.aivle.bigproject.dto.repo;

/**
 * GitHub Pull Request 생성 결과를 반환하기 위한 응답 DTO.
 */
public record GithubPullRequestResult(
        Integer number,
        String url,
        String status,
        boolean draft,
        String headCommitSha
) {}
