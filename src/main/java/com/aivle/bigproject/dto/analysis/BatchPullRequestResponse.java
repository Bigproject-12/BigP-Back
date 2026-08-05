package com.aivle.bigproject.dto.analysis;

import java.util.List;

public record BatchPullRequestResponse(
        Integer pullRequestNumber,
        String pullRequestUrl,
        String headBranch,
        String baseBranch,
        String headCommitSha,
        List<Integer> analysisIds,
        boolean recovered,
        String status
) {
}
