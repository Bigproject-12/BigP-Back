package com.aivle.bigproject.dto.repo;

import com.aivle.bigproject.entity.GithubRepo;
import java.time.LocalDate;

/**
 * GitHub 저장소 정보를 프론트엔드에 전달하는 응답 DTO(목록/상세 조회 공용)
 */

public record RepoResponse (
    Integer id,
    String name,
    String repoUrl,
    String language,
    String organization,
    String lastUpdated,
    Boolean isPrivate,
    LocalDate createdAt
){
    //GithubRepo 엔티티 -> RepoResponse DTO 변환 (UserResponse.from()과 동일 패턴)
    public static RepoResponse from(GithubRepo repo){
        return new RepoResponse(
                repo.getId(),
                repo.getName(),
                repo.getRepoUrl(),
                repo.getLanguage(),
                repo.getOrganization(),
                repo.getLastUpdated(),
                repo.getIsPrivate(),
                repo.getCreatedAt()
        );
    }
}
