package com.aivle.bigproject.dto.repo;

import java.time.OffsetDateTime;
/**
 * GitHub Pull Request 정보를 반환하기 위한 응답 DTO.
 *
 * PR 번호, 제목, 작성자, 상태 등의 정보를 포함
 */
public record GithubPullRequestResponse(
        Integer githubPrNumber,
        String title,
        String author,
        String status,
        String prUrl,
        String headBranch,
        String baseBranch,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime mergedAt,
        boolean platformGenerated
) {}
