package com.aivle.bigproject.dto.repo;

/** GitHub PR 생성 API 응답에서 DB 저장에 필요한 값. */
public record GithubPullRequestResult(
        Integer number,
        String url,
        String status,
        boolean draft,
        String headCommitSha
) {}
