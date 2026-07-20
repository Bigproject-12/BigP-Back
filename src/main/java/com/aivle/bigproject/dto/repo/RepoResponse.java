package com.aivle.bigproject.dto.repo;

import com.aivle.bigproject.entity.GithubRepo;
import java.time.LocalDate;

/**
 * 레포 정보를 프론트로 돌려줄 응답 DTO (목록/상세 조회 공용)
 *
 * TODO: 레포 '연동 요청' DTO(RepoConnectRequest)는 보류 중.
 *   - 이유: 연동 흐름 미정 (URL 직접 입력 vs GitHub 계정 연동 후 목록에서 선택)
 *           + GitHub 토큰 전략 미정 (PAT / OAuth / GitHub App)
 */

 public record RepoResponse (
    Integer id,
    String name,
    String repoUrl,
    String language,    
    String lastUpdated,
    LocalDate createdAt
){
    // 엔티티 -> DTO 변환 (UserResponse.from()과 동일 패턴)
    public static RepoResponse from(GithubRepo repo){
        return new RepoResponse(
                repo.getId(),
                repo.getName(),
                repo.getRepoUrl(),
                repo.getLanguage(), 
                repo.getLastUpdated(),
                repo.getCreatedAt()
        );
    }
}
