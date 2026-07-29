package com.aivle.bigproject.dto.repo;

import java.time.OffsetDateTime;

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
