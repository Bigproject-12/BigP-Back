package com.aivle.bigproject.dto.analysis;

import java.util.List;

public record BatchPushPreparationResponse(
        List<Integer> analysisIds,
        Integer repoId,
        String repository,
        String branch,
        String branchHeadSha,
        String status
) {
}
