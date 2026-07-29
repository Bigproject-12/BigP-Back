package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.GithubPullRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    List<GithubPullRequest>
            findTop10ByUser_Company_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                    Integer companyId,
                    LocalDateTime from,
                    LocalDateTime toExclusive);

    Optional<GithubPullRequest>
            findByGithubRepo_OrganizationAndGithubRepo_NameAndGithubPrNumber(
                    String organization,
                    String repoName,
                    Integer githubPrNumber);
}
