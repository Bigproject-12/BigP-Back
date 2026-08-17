package com.aivle.bigproject.dto.repo;

/**
 * GitHub 저장소의 브랜치 정보를 반환하기 위한 응답 DTO.
 *
 * 브랜치 이름과 최신 Commit SHA를 포함하며,
 * 보호 브랜치 여부와 기본 브랜치 여부를 나타내는 필드를 포함
 */
public record BranchResponse(
        String name,
        String commitSha,
        boolean protectedBranch,
        boolean isDefault
) {}
