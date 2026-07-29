package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.analysis.DashboardResponse;
import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.GithubPullRequest;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.FindingRepository;
import com.aivle.bigproject.repository.GithubPullRequestRepository;
import com.aivle.bigproject.repository.UserRepository;
import com.aivle.bigproject.repository.UserRepoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 대시보드 출력하고자하는 데이터를 조회하는 서비스
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AnalysisRepository analysisRepository;
    private final FindingRepository findingRepository;
    private final UserRepository userRepository;
    private final GithubPullRequestRepository githubPullRequestRepository;
    private final UserRepoRepository userRepoRepository;

    public DashboardService(
            AnalysisRepository analysisRepository,
            FindingRepository findingRepository,
            UserRepository userRepository,
            GithubPullRequestRepository githubPullRequestRepository,
            UserRepoRepository userRepoRepository
    ) {
        this.analysisRepository = analysisRepository;
        this.findingRepository = findingRepository;
        this.userRepository = userRepository;
        this.githubPullRequestRepository = githubPullRequestRepository;
        this.userRepoRepository = userRepoRepository;
    }

    // 필요한 데이터를 조회하여 DashboardResponse 객체를 생성하고 반환
    // 대시보드는 로그인한 유저 개인이 아니라 그 유저가 속한 회사(company_id) 전체 집계다 —
    // 같은 회사 팀원이라면 누가 분석을 돌렸든 같은 대시보드를 봐야 한다.
    public DashboardResponse getDashboard(Integer userId, LocalDate from, LocalDate to) {
        // 기간이 지정되지 않은 경우 오늘을 기준으로 조회하도록 설정
        LocalDate endDate = to != null ? to : LocalDate.now();
        //기간이 지정되지 않으면 종료일을 포함하여 최근 7일간 조회하도록 설정
        LocalDate startDate = from != null ? from : endDate.minusDays(6);
        if (startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

        Integer companyId = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                .getCompany().getId();

        LocalDateTime fromDateTime = startDate.atStartOfDay();
        LocalDateTime toExclusive = endDate.plusDays(1).atStartOfDay();
        long periodDays = startDate.datesUntil(endDate.plusDays(1)).count();
        LocalDateTime previousFrom = startDate.minusDays(periodDays).atStartOfDay();
        LocalDateTime previousToExclusive = fromDateTime;

        //현재 기간의 전체 이슈, 보안 이슈, 비효율 이슈 개수를 조회
        FindingRepository.IssueCountSummary issueCounts =
                findingRepository.sumIssueCountsByCompanyIdAndPeriod(
                        companyId, fromDateTime, toExclusive);
        // 이전 기간의 이슈 수를 조회
        FindingRepository.IssueCountSummary previousIssueCounts =
                findingRepository.sumIssueCountsByCompanyIdAndPeriod(
                        companyId, previousFrom, previousToExclusive);
        //현재 기간의 분석 수를 조회
        long analysisCount = analysisRepository.countByCompanyIdAndPeriod(
                companyId, fromDateTime, toExclusive);
        // 이전 기간의 분석 수를 조회
        long previousAnalysisCount = analysisRepository.countByCompanyIdAndPeriod(
                companyId, previousFrom, previousToExclusive);
        //현재 기간의 평균 품질 점수를 조회
        double averageQualityScore = findingRepository.averageQualityScoreByCompanyIdAndPeriod(
                companyId, fromDateTime, toExclusive);
        // 이전 기간의 평균 품질 점수를 조회
        double previousQualityScore = findingRepository.averageQualityScoreByCompanyIdAndPeriod(
                companyId, previousFrom, previousToExclusive);
        long pullRequestCount = githubPullRequestRepository.countByCompanyIdAndPeriod(
                companyId, fromDateTime, toExclusive);
        long previousPullRequestCount = githubPullRequestRepository.countByCompanyIdAndPeriod(
                companyId, previousFrom, previousToExclusive);

        // 최신 분석 데이터를 조회
        List<DashboardResponse.RecentAnalysis> recentAnalyses = analysisRepository
                .findTop5ByCompanyIdAndPeriod(companyId, fromDateTime, toExclusive)
                .stream()
                .map(this::toRecentAnalysis)
                .toList();

        Map<LocalDate, FindingRepository.DailyQualityScore> dailyScores = findingRepository
                .findDailyQualityScoresByCompanyIdAndPeriod(companyId, fromDateTime, toExclusive)
                .stream()
                .collect(Collectors.toMap(
                        FindingRepository.DailyQualityScore::getAnalysisDate,
                        Function.identity()));
        List<DashboardResponse.QualityTrend> qualityTrend = startDate
                .datesUntil(endDate.plusDays(1))
                .map(date -> new DashboardResponse.QualityTrend(
                        date,
                        dailyScores.containsKey(date) ? dailyScores.get(date).getAverageScore() : null))
                .toList();

        long otherIssueCount = Math.max(0,
                issueCounts.getTotalIssueCount()
                        - issueCounts.getSecurityIssueCount()
                        - issueCounts.getInefficiencyIssueCount());
        List<DashboardResponse.IssueDistribution> issueDistribution = List.of(
                distribution("SECURITY", issueCounts.getSecurityIssueCount(), issueCounts.getTotalIssueCount()),
                distribution("INEFFICIENCY", issueCounts.getInefficiencyIssueCount(), issueCounts.getTotalIssueCount()),
                distribution("OTHER", otherIssueCount, issueCounts.getTotalIssueCount())
        );
        List<FindingRepository.RiskRepositorySummary> riskSummaries = findingRepository
                .findTop5RiskRepositoriesByCompanyIdAndPeriod(companyId, fromDateTime, toExclusive);
        List<DashboardResponse.RiskRepository> riskRepositories = IntStream
                .range(0, riskSummaries.size())
                .mapToObj(index -> toRiskRepository(index + 1, riskSummaries.get(index)))
                .toList();
        List<DashboardResponse.RecentPullRequest> recentPullRequests = githubPullRequestRepository
                .findTop10ByUser_Company_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        companyId, fromDateTime, toExclusive)
                .stream()
                .map(this::toRecentPullRequest)
                .toList();

        // 대시보드 응답 데이터를 생성하여, 반환
        return new DashboardResponse(
                userRepoRepository.countDistinctRepoByCompanyId(companyId),
                analysisCount,
                analysisRepository.countByCompanyIdAndStatusAndPeriod(
                        companyId, "ANALYZING", fromDateTime, toExclusive),
                analysisRepository.countByCompanyIdAndStatusAndPeriod(
                        companyId, "COMPLETED", fromDateTime, toExclusive),
                analysisRepository.countByCompanyIdAndStatusAndPeriod(
                        companyId, "FAILED", fromDateTime, toExclusive),
                analysisRepository.countByCompanyIdAndStatusAndPeriod(
                        companyId, "CANCELED", fromDateTime, toExclusive),
                issueCounts.getTotalIssueCount(),
                issueCounts.getSecurityIssueCount(),
                issueCounts.getInefficiencyIssueCount(),
                averageQualityScore,
                pullRequestCount,
                githubPullRequestRepository.countByCompanyIdAndStatusAndPeriod(
                        companyId, "OPEN", fromDateTime, toExclusive),
                githubPullRequestRepository.countByCompanyIdAndStatusAndPeriod(
                        companyId, "MERGED", fromDateTime, toExclusive),
                githubPullRequestRepository.countByCompanyIdAndStatusAndPeriod(
                        companyId, "CLOSED", fromDateTime, toExclusive),
                new DashboardResponse.Comparison(
                        changeRate(analysisCount, previousAnalysisCount),
                        changeRate(issueCounts.getTotalIssueCount(), previousIssueCounts.getTotalIssueCount()),
                        roundOneDecimal(averageQualityScore - previousQualityScore),
                        changeRate(pullRequestCount, previousPullRequestCount)
                ),
                qualityTrend,
                issueDistribution,
                riskRepositories,
                recentAnalyses,
                recentPullRequests
        );
    }

    private DashboardResponse.RecentPullRequest toRecentPullRequest(GithubPullRequest pullRequest) {
        return new DashboardResponse.RecentPullRequest(
                pullRequest.getId(),
                pullRequest.getGithubPrNumber(),
                pullRequest.getGithubRepo().getId(),
                pullRequest.getGithubRepo().getName(),
                pullRequest.getTitle(),
                pullRequest.getStatus(),
                pullRequest.getPrUrl(),
                pullRequest.getHeadBranch(),
                pullRequest.getBaseBranch(),
                pullRequest.getCreatedAt()
        );
    }

    private DashboardResponse.RiskRepository toRiskRepository(
            int rank,
            FindingRepository.RiskRepositorySummary summary
    ) {
        return new DashboardResponse.RiskRepository(
                rank,
                summary.getRepoId(),
                summary.getRepoName(),
                summary.getQualityScore(),
                summary.getTotalIssueCount(),
                summary.getSecurityIssueCount(),
                summary.getInefficiencyIssueCount(),
                summary.getOtherIssueCount(),
                summary.getLastAnalyzedAt()
        );
    }

    private DashboardResponse.IssueDistribution distribution(String type, long count, long total) {
        double percentage = total == 0 ? 0 : roundOneDecimal(count * 100.0 / total);
        return new DashboardResponse.IssueDistribution(type, count, percentage);
    }

    private double changeRate(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0 : 100;
        }
        return roundOneDecimal((current - previous) * 100.0 / previous);
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
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
