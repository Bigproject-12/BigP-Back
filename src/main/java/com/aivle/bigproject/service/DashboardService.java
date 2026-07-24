package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.analysis.DashboardResponse;
import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.FindingRepository;
import com.aivle.bigproject.repository.UserRepoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 대시보드 출력하고자하는 데이터를 조회하는 서비스
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AnalysisRepository analysisRepository;
    private final FindingRepository findingRepository;
    private final UserRepoRepository userRepoRepository;

    public DashboardService(
            AnalysisRepository analysisRepository,
            FindingRepository findingRepository,
            UserRepoRepository userRepoRepository
    ) {
        this.analysisRepository = analysisRepository;
        this.findingRepository = findingRepository;
        this.userRepoRepository = userRepoRepository;
    }

    // 필요한 데이터를 조회하여 DashboardResponse 객체를 생성하고 반환
    public DashboardResponse getDashboard(Integer userId) {
        FindingRepository.IssueCountSummary issueCounts =
                findingRepository.sumIssueCountsByUserId(userId);

        // 최신 분석 데이터를 조회
        List<DashboardResponse.RecentAnalysis> recentAnalyses = analysisRepository
                .findTop5ByUserIdOrderByIdDesc(userId)
                .stream()
                .map(this::toRecentAnalysis)
                .toList();

        // 대시보드 응답 데이터를 생성하여, 반환
                return new DashboardResponse(
                userRepoRepository.countByUserId(userId),
                analysisRepository.countByUserId(userId),
                analysisRepository.countByUserIdAndStatus(userId, "ANALYZING"),
                analysisRepository.countByUserIdAndStatus(userId, "COMPLETED"),
                analysisRepository.countByUserIdAndStatus(userId, "FAILED"),
                analysisRepository.countByUserIdAndStatus(userId, "CANCELED"),
                issueCounts.getTotalIssueCount(),
                issueCounts.getSecurityIssueCount(),
                issueCounts.getInefficiencyIssueCount(),
                recentAnalyses
        );
    }

    // Analysis 엔티티를 DashboardResponse.RecentAnalysis DTO로 변환
    private DashboardResponse.RecentAnalysis toRecentAnalysis(Analysis analysis) {
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
