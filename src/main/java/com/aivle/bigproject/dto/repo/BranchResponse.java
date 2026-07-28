package com.aivle.bigproject.dto.repo;

public record BranchResponse(
        String name,
        String commitSha,
        boolean protectedBranch
) {}
