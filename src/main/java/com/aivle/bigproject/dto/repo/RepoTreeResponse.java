package com.aivle.bigproject.dto.repo;

import java.util.List;

public record RepoTreeResponse(
        Integer repoId,
        String branch,
        boolean truncated,
        List<Item> items
) {
    public record Item(
            String path,
            String type,
            String sha,
            Long size,
            int totalIssueCount,
            int securityIssueCount,
            int inefficiencyIssueCount,
            int otherIssueCount
    ) {}
}
