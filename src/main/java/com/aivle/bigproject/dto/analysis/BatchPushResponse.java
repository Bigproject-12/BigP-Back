package com.aivle.bigproject.dto.analysis;

import java.time.LocalDateTime;
import java.util.List;

public record BatchPushResponse(
        List<Integer> analysisIds,
        Integer repoId,
        String repository,
        String branch,
        String branchHeadSha,
        String baseTreeSha,
        String treeSha,
        String commitSha,
        LocalDateTime pushedAt,
        List<FileBlob> files,
        String status
) {
    public record FileBlob(
            Integer analysisId,
            String filePath,
            String blobSha
    ) {
    }
}
