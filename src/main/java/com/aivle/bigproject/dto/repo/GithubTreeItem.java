package com.aivle.bigproject.dto.repo;

/**
 * GitHub Tree Item 정보를 반환하기 위한 응답 DTO.
 */
public record GithubTreeItem(
        String path,
        String blobSha,
        String mode
) {
}
