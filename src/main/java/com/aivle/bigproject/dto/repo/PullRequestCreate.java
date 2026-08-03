package com.aivle.bigproject.dto.repo;

public record PullRequestCreate(
        String baseBranch,
        String title,
        String body
) {}