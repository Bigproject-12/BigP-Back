package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

//Finding 엔티티에 대한 데이터 조회 및 집계 기능을 제공
public interface FindingRepository extends JpaRepository<Finding, Integer> {
        //유저의 모든 분석 결과의 이슈 수를 합산하여 반환
    @Modifying
    @Query(value = """
            DELETE FROM FINDING
            WHERE analysis_id IN (
                SELECT analysis_id FROM ANALYSIS WHERE user_id = :userId
            )
            """, nativeQuery = true)
    void deleteByAnalysisOwner(@Param("userId") Integer userId);

    //유저별 대시보드 정보를 조회
    Optional<Finding> findByAnalysisId(Integer analysisId);

    // 유저별 대시보드 통계 정보를 조회
    @Query("""
            SELECT COALESCE(SUM(f.totalIssues), 0) AS totalIssueCount,
                   COALESCE(SUM(f.securityCount), 0) AS securityIssueCount,
                   COALESCE(SUM(f.inefficiencyCount), 0) AS inefficiencyIssueCount
            FROM Finding f
            WHERE f.analysis.user.id = :userId
            """)
    IssueCountSummary sumIssueCountsByUserId(@Param("userId") Integer userId);

    // 유저별 기간별 이슈 수를 조회
    @Query(value = """
            SELECT COALESCE(SUM(f.total_issues), 0) AS totalIssueCount,
                   COALESCE(SUM(f.security_count), 0) AS securityIssueCount,
                   COALESCE(SUM(f.inefficiency_count), 0) AS inefficiencyIssueCount
            FROM FINDING f
            JOIN ANALYSIS a ON a.analysis_id = f.analysis_id
            WHERE a.user_id = :userId
              AND a.created_at >= :from
              AND a.created_at < :toExclusive
            """, nativeQuery = true)
    IssueCountSummary sumIssueCountsByUserIdAndPeriod(
            @Param("userId") Integer userId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    // 유저별 기간별 평균 품질 점수를 조회
    @Query(value = """
            SELECT COALESCE(ROUND(AVG(GREATEST(0,
                       100 - f.security_count * 10
                           - f.inefficiency_count * 5
                           - GREATEST(f.total_issues - f.security_count - f.inefficiency_count, 0) * 3
                   )), 1), 0) AS averageScore
            FROM FINDING f
            JOIN ANALYSIS a ON a.analysis_id = f.analysis_id
            WHERE a.user_id = :userId
              AND a.created_at >= :from
              AND a.created_at < :toExclusive
            """, nativeQuery = true)
    double averageQualityScoreByUserIdAndPeriod(
            @Param("userId") Integer userId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    // 유저별 기간별 일일 품질 점수를 조회
    @Query(value = """
            SELECT DATE(a.created_at) AS analysisDate,
                   ROUND(AVG(GREATEST(0,
                       100 - f.security_count * 10
                           - f.inefficiency_count * 5
                           - GREATEST(f.total_issues - f.security_count - f.inefficiency_count, 0) * 3
                   )), 1) AS averageScore
            FROM FINDING f
            JOIN ANALYSIS a ON a.analysis_id = f.analysis_id
            WHERE a.user_id = :userId
              AND a.created_at >= :from
              AND a.created_at < :toExclusive
            GROUP BY DATE(a.created_at)
            ORDER BY DATE(a.created_at)
            """, nativeQuery = true)
    List<DailyQualityScore> findDailyQualityScoresByUserIdAndPeriod(
            @Param("userId") Integer userId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    interface IssueCountSummary {
        long getTotalIssueCount();
        long getSecurityIssueCount();
        long getInefficiencyIssueCount();
    }

    // 유저별 기간별 일일 품질 점수를 조회하는 DTO 인터페이스
    interface DailyQualityScore {
        LocalDate getAnalysisDate();
        double getAverageScore();
    }
}
