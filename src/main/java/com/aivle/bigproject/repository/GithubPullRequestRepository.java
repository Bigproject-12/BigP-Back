package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.GithubPullRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
// GitHub Pull Request 정보를 관리하는 Repository       
public interface GithubPullRequestRepository
        extends JpaRepository<GithubPullRequest, Integer> {
// 특정 기간 내 회사의 전체 PR 개수 조회
    @Query("""
            SELECT COUNT(pr)
            FROM GithubPullRequest pr
            WHERE pr.user.company.id = :companyId
              AND pr.createdAt >= :from
              AND pr.createdAt < :toExclusive
            """)
    long countByCompanyIdAndPeriod(
            @Param("companyId") Integer companyId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);
// 특정 기간 내 회사의 상태별 PR 개수 조회
    @Query("""
            SELECT COUNT(pr)
            FROM GithubPullRequest pr
            WHERE pr.user.company.id = :companyId
              AND pr.status = :status
              AND pr.createdAt >= :from
              AND pr.createdAt < :toExclusive
            """)
    long countByCompanyIdAndStatusAndPeriod(
            @Param("companyId") Integer companyId,
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);
// 특정 기간 내 회사의 최근 PR 조회
    @Query("""
            SELECT pr
            FROM GithubPullRequest pr
            WHERE pr.user.company.id = :companyId
              AND pr.createdAt >= :from
              AND pr.createdAt < :toExclusive
            ORDER BY pr.createdAt DESC
            """)
    List<GithubPullRequest> findRecentByCompanyIdAndPeriod(
            @Param("companyId") Integer companyId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            Pageable pageable);
// 특정 기간 내 사용자의 최근 PR 조회
    @Query("""
            SELECT pr
            FROM GithubPullRequest pr
            WHERE pr.user.id = :userId
              AND pr.createdAt >= :from
              AND pr.createdAt < :toExclusive
            ORDER BY pr.createdAt DESC
            """)
    List<GithubPullRequest> findRecentByUserIdAndPeriod(
            @Param("userId") Integer userId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            Pageable pageable);
// 저장소와 GitHub PR 번호를 기준으로 관리 중인 PR 조회
    @Query("""
            SELECT pr
            FROM GithubPullRequest pr
            WHERE pr.githubRepo.organization = :organization
              AND pr.githubRepo.name = :repoName
              AND pr.githubPrNumber = :githubPrNumber
            """)
    Optional<GithubPullRequest> findTrackedPullRequest(
            @Param("organization") String organization,
            @Param("repoName") String repoName,
            @Param("githubPrNumber") Integer githubPrNumber);
// 저장소의 PR 번호 중 플랫폼에서 생성한 PR 번호 조회
    @Query("""
            SELECT pr.githubPrNumber
            FROM GithubPullRequest pr
            WHERE pr.githubRepo.id = :repoId
              AND pr.githubPrNumber IN :githubPrNumbers
            """)
    Set<Integer> findPlatformGeneratedNumbers(
            @Param("repoId") Integer repoId,
            @Param("githubPrNumbers") Set<Integer> githubPrNumbers);
}
