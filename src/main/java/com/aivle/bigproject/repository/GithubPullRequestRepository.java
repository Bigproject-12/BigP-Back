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

public interface GithubPullRequestRepository
        extends JpaRepository<GithubPullRequest, Integer> {

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
