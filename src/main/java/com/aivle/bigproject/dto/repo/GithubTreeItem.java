package com.aivle.bigproject.dto.repo;

public record GithubTreeItem(
        String path,
        String blobSha,
        String mode
) {
}
