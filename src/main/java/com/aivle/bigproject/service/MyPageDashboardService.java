
package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.analysis.DashboardResponse;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.FindingRepository;
import com.aivle.bigproject.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MyPageDashboardService {

    private final AnalysisRepository analysisRepository;
    private final FindingRepository findingRepository;
    private final UserRepository userRepository;

    public MyPageDashboardService(
            AnalysisRepository analysisRepository,
            FindingRepository findingRepository,
            UserRepository userRepository
    ) {
        this.analysisRepository = analysisRepository;
        this.findingRepository = findingRepository;
        this.userRepository = userRepository;
    }

    public DashboardResponse getRepoDashboard(Integer userId, Integer repoId, LocalDate from, LocalDate to) {
        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(6);
        if (startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime fromDateTime = startDate.atStartOfDay();
        LocalDateTime toExclusive = endDate.plusDays(1).atStartOfDay();

        // 레포지토리 기준 통계 조회 (Repository에 repoId 전용 쿼리 구현 필요)
        FindingRepository.IssueCountSummary issueCounts =
                findingRepository.sumIssueCountsByRepoIdAndPeriod(repoId, fromDateTime, toExclusive);

        long analysisCount = analysisRepository.countByRepoIdAndPeriod(repoId, fromDateTime, toExclusive);
        double averageQualityScore = findingRepository.averageQualityScoreByRepoIdAndPeriod(repoId, fromDateTime, toExclusive);

        List<DashboardResponse.RecentAnalysis> recentAnalyses = analysisRepository
                .findTop5ByRepoIdAndPeriod(repoId, fromDateTime, toExclusive)
                .stream()
                .map(this::toRecentAnalysis)
                .toList();

        return new DashboardResponse(
                1,
                analysisCount,
                analysisRepository.countByRepoIdAndStatusAndPeriod(repoId, "ANALYZING", fromDateTime, toExclusive),
                analysisRepository.countByRepoIdAndStatusAndPeriod(repoId, "COMPLETED", fromDateTime, toExclusive),
                analysisRepository.countByRepoIdAndStatusAndPeriod(repoId, "FAILED", fromDateTime, toExclusive),
                analysisRepository.countByRepoIdAndStatusAndPeriod(repoId, "CANCELED", fromDateTime, toExclusive),
                issueCounts.getTotalIssueCount(),
                issueCounts.getSecurityIssueCount(),
                issueCounts.getInefficiencyIssueCount(),
                averageQualityScore,
                null,
                List.of(),
                List.of(),
                List.of(),
                recentAnalyses
        );
    }

    private DashboardResponse.RecentAnalysis toRecentAnalysis(com.aivle.bigproject.entity.Analysis analysis) {
        long totalIssueCount = findingRepository.findByAnalysisId(analysis.getId())
                .map(finding -> finding.getTotalIssues().longValue())
                .orElse(0L);

        return new DashboardResponse.RecentAnalysis(
                analysis.getId(),
                analysis.getGithubRepo().getId(),
                analysis.getGithubRepo().getName(),
                analysis.getLanguage(),
                analysis.getStatus(),
                totalIssueCount
        );
    }
}