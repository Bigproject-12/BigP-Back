package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.myspace.MySpaceAnalysisResponse;
import com.aivle.bigproject.dto.myspace.MySpaceAnalysisDetailResponse;
import com.aivle.bigproject.dto.myspace.MySpaceSummaryResponse;
import com.aivle.bigproject.dto.myspace.MySpaceOverviewResponse;
import com.aivle.bigproject.dto.repo.GithubPullRequestResponse;
import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Finding;
import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.UserRepo;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.entity.GithubPullRequest;
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.FindingRepository;
import com.aivle.bigproject.repository.GithubPullRequestRepository;
import com.aivle.bigproject.repository.UserRepoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.type.TypeReference;

@Service
@Transactional(readOnly = true)
public class MySpaceService {

    private final UserRepoRepository userRepoRepository;
    private final AnalysisRepository analysisRepository;
    private final FindingRepository findingRepository;
    private final GithubPullRequestRepository githubPullRequestRepository;
    private final JsonMapper jsonMapper;
    private final GithubService githubService;

    public MySpaceService(
            UserRepoRepository userRepoRepository,
            AnalysisRepository analysisRepository,
            FindingRepository findingRepository,
            GithubPullRequestRepository githubPullRequestRepository,
            JsonMapper jsonMapper,
            GithubService githubService
    ) {
        this.userRepoRepository = userRepoRepository;
        this.analysisRepository = analysisRepository;
        this.findingRepository = findingRepository;
        this.githubPullRequestRepository = githubPullRequestRepository;
        this.jsonMapper = jsonMapper;
        this.githubService = githubService;
    }

    public MySpaceOverviewResponse getOverview(Integer userId, LocalDate from, LocalDate to) {
        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(6);
        if (startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

        LocalDateTime fromDateTime = startDate.atStartOfDay();
        LocalDateTime toExclusive = endDate.plusDays(1).atStartOfDay();
        long periodDays = startDate.datesUntil(endDate.plusDays(1)).count();
        LocalDateTime previousFrom = startDate.minusDays(periodDays).atStartOfDay();
        LocalDateTime previousToExclusive = fromDateTime;

        long repositoryCount = userRepoRepository.countByUserId(userId);

        long totalAnalysisCount = analysisRepository.countByUserIdAndPeriod(userId, fromDateTime, toExclusive);
        long previousAnalysisCount = analysisRepository.countByUserIdAndPeriod(
                userId, previousFrom, previousToExclusive);

        FindingRepository.IssueCountSummary issueCounts =
                findingRepository.sumIssueCountsByUserIdAndPeriod(userId, fromDateTime, toExclusive);
        FindingRepository.IssueCountSummary previousIssueCounts =
                findingRepository.sumIssueCountsByUserIdAndPeriod(userId, previousFrom, previousToExclusive);

        double averageQualityScore = findingRepository.averageQualityScoreByUserIdAndPeriod(
                userId, fromDateTime, toExclusive);
        double previousQualityScore = findingRepository.averageQualityScoreByUserIdAndPeriod(
                userId, previousFrom, previousToExclusive);

        Map<LocalDate, FindingRepository.DailyQualityScore> dailyScores = findingRepository
                .findDailyQualityScoresByUserIdAndPeriod(userId, fromDateTime, toExclusive)
                .stream()
                .collect(Collectors.toMap(
                        FindingRepository.DailyQualityScore::getAnalysisDate,
                        Function.identity()));
        List<MySpaceOverviewResponse.QualityTrend> qualityTrend = startDate
                .datesUntil(endDate.plusDays(1))
                .map(date -> new MySpaceOverviewResponse.QualityTrend(
                        date,
                        dailyScores.containsKey(date) ? dailyScores.get(date).getAverageScore() : null))
                .toList();

        long otherIssueCount = Math.max(0,
                issueCounts.getTotalIssueCount()
                        - issueCounts.getSecurityIssueCount()
                        - issueCounts.getInefficiencyIssueCount());
        List<MySpaceOverviewResponse.IssueDistribution> issueDistribution = List.of(
                distribution("SECURITY", issueCounts.getSecurityIssueCount(), issueCounts.getTotalIssueCount()),
                distribution("INEFFICIENCY", issueCounts.getInefficiencyIssueCount(), issueCounts.getTotalIssueCount()),
                distribution("OTHER", otherIssueCount, issueCounts.getTotalIssueCount())
        );

        List<FindingRepository.RiskRepositorySummary> riskSummaries = findingRepository
                .findTop5RiskRepositoriesByUserIdAndPeriod(userId, fromDateTime, toExclusive);
        List<MySpaceOverviewResponse.RiskRepository> riskRepositories = IntStream
                .range(0, riskSummaries.size())
                .mapToObj(index -> toRiskRepository(index + 1, riskSummaries.get(index)))
                .toList();

        List<MySpaceOverviewResponse.RecentAnalysis> recentAnalyses = analysisRepository
                .findTop5ByUserIdAndPeriod(userId, fromDateTime, toExclusive)
                .stream()
                .map(this::toRecentAnalysis)
                .toList();

        List<MySpaceOverviewResponse.RecentPullRequest> recentPullRequests = githubPullRequestRepository
                .findRecentByUserIdAndPeriod(userId, fromDateTime, toExclusive, PageRequest.of(0, 10))
                .stream()
                .map(this::toRecentPullRequest)
                .toList();

        return new MySpaceOverviewResponse(
                repositoryCount,
                totalAnalysisCount,
                issueCounts.getTotalIssueCount(),
                averageQualityScore,
                new MySpaceOverviewResponse.Comparison(
                        changeRate(totalAnalysisCount, previousAnalysisCount),
                        changeRate(issueCounts.getTotalIssueCount(), previousIssueCounts.getTotalIssueCount()),
                        roundOneDecimal(averageQualityScore - previousQualityScore)),
                qualityTrend,
                issueDistribution,
                riskRepositories,
                recentAnalyses,
                recentPullRequests);
    }

    private MySpaceOverviewResponse.RecentPullRequest toRecentPullRequest(GithubPullRequest pullRequest) {
        return new MySpaceOverviewResponse.RecentPullRequest(
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

    private MySpaceOverviewResponse.RiskRepository toRiskRepository(
            int rank,
            FindingRepository.RiskRepositorySummary summary
    ) {
        return new MySpaceOverviewResponse.RiskRepository(
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

    private MySpaceOverviewResponse.IssueDistribution distribution(String type, long count, long total) {
        double percentage = total == 0 ? 0 : roundOneDecimal(count * 100.0 / total);
        return new MySpaceOverviewResponse.IssueDistribution(type, count, percentage);
    }

    private MySpaceOverviewResponse.RecentAnalysis toRecentAnalysis(Analysis analysis) {
        long totalIssueCount = findingRepository.findByAnalysisId(analysis.getId())
                .map(finding -> finding.getTotalIssues().longValue())
                .orElse(0L);

        return new MySpaceOverviewResponse.RecentAnalysis(
                analysis.getId(),
                analysis.getGithubRepo().getId(),
                analysis.getGithubRepo().getName(),
                analysis.getLanguage(),
                analysis.getStatus(),
                totalIssueCount
        );
    }

    public MySpaceSummaryResponse getSummary(Integer userId, Integer repoId, String branch) {
        validateBranch(branch);
        UserRepo userRepo = getConnectedRepo(userId, repoId);
        GithubRepo repo = userRepo.getGithubRepo();
        long totalAnalysisCount = analysisRepository.countByUser_IdAndGithubRepo_IdAndBranch(userId, repoId, branch);
        List<Analysis> analyses = analysisRepository.findLatestTwoPerFile(userId, repoId, branch);
        if (analyses.isEmpty()) {
            return emptySummary(repo, branch, totalAnalysisCount);
        }

        Map<String, List<Analysis>> analysesByFile = analyses.stream()
                .collect(Collectors.groupingBy(
                        Analysis::getFilePath,
                        LinkedHashMap::new,
                        Collectors.toList()));
        List<Analysis> currentAnalyses = analysesByFile.values().stream()
                .map(fileAnalyses -> fileAnalyses.get(0))
                .toList();
        List<Analysis> previousAnalyses = analysesByFile.values().stream()
                .filter(fileAnalyses -> fileAnalyses.size() > 1)
                .map(fileAnalyses -> fileAnalyses.get(1))
                .toList();
        Map<Integer, Finding> findings = findingsByAnalysisId(analyses);
        Aggregate currentAggregate = aggregate(currentAnalyses, findings);
        Aggregate previousAggregate = aggregate(previousAnalyses, findings);
        Analysis latest = currentAnalyses.get(0);
        Finding latestFinding = findings.get(latest.getId());

        return new MySpaceSummaryResponse(
                repo.getId(),
                repo.getName(),
                branch,
                latest.getId(),
                latest.getCreatedAt(),
                latest.getFilePath(),
                totalAnalysisCount,
                currentAggregate.totalIssues(),
                currentAggregate.securityIssues(),
                currentAggregate.inefficiencyIssues(),
                currentAggregate.otherIssues(),
                currentAggregate.improvableRatio(),
                currentAggregate.qualityScore(),
                comparison(currentAggregate, previousAggregate, previousAnalyses.isEmpty()),
                latestFinding == null ? null : latestFinding.getSecuResult(),
                latestFinding == null ? null : latestFinding.getInefficiencyResult(),
                latest.getOriginCode(),
                latestFinding == null ? null : latestFinding.getModifiedCode()
        );
    }

    public List<MySpaceAnalysisResponse> getAnalyses(
            Integer userId,
            Integer repoId,
            String branch,
            int page,
            int size
    ) {
        validateBranch(branch);
        if (page < 1 || size < 1 || size > 50) {
            throw new CustomException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        getConnectedRepo(userId, repoId);
        List<Analysis> analyses = analysisRepository.findMySpaceHistory(
                userId, repoId, branch, PageRequest.of(page - 1, size));
        if (analyses.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, Finding> findings = findingRepository
                .findAllByAnalysisIdIn(analyses.stream().map(Analysis::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        finding -> finding.getAnalysis().getId(),
                        Function.identity(),
                        (first, ignored) -> first));

        return analyses.stream()
                .map(analysis -> {
                    Finding finding = findings.get(analysis.getId());
                    return new MySpaceAnalysisResponse(
                            analysis.getId(),
                            analysis.getFilePath(),
                            analysis.getBranch(),
                            analysis.getStatus(),
                            analysis.getCreatedAt(),
                            finding == null
                                    ? "COMPLETED".equals(analysis.getStatus()) ? 0 : null
                                    : finding.getTotalIssues(),
                            finding == null ? null : qualityScore(finding),
                            analysis.getImprovableRatio()
                    );
                })
                .toList();
    }

    public MySpaceAnalysisDetailResponse getLatestFileAnalysis(
            Integer userId,
            Integer repoId,
            String branch,
            String filePath
    ) {
        validateBranch(branch);
        if (filePath == null || filePath.isBlank()) {
            throw new CustomException(ErrorCode.ANALYSIS_BRANCH_FILE_INFO_MISSING);
        }
        getConnectedRepo(userId, repoId);
        Analysis analysis = analysisRepository.findLatestCompletedFile(
                        userId, repoId, branch, filePath, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_NOT_FOUND));
        return detail(analysis);
    }

    public MySpaceAnalysisDetailResponse getAnalysisDetail(Integer userId, Integer analysisId) {
        Analysis analysis = analysisRepository.findOwnedAnalysis(analysisId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_NOT_FOUND));
        return detail(analysis);
    }

    public List<GithubPullRequestResponse> getMyPullRequests(
            Integer userId,
            Integer repoId,
            String branch,
            String status,
            int limit
    ) {
        String normalizedStatus = status == null ? "ALL" : status.toUpperCase(Locale.ROOT);
        if (!List.of("ALL", "OPEN", "MERGED", "CLOSED").contains(normalizedStatus)
                || limit < 1 || limit > 50) {
            throw new CustomException(ErrorCode.INVALID_PR_QUERY);
        }
        String githubState = switch (normalizedStatus) {
            case "OPEN" -> "open";
            case "MERGED", "CLOSED" -> "closed";
            default -> "all";
        };
        return githubService.getRepositoryPullRequests(
                        userId, repoId, "mine", githubState, 1, 50)
                .stream()
                .filter(pr -> "ALL".equals(normalizedStatus)
                        || normalizedStatus.equals(pr.status()))
                .filter(pr -> branch == null || branch.isBlank()
                        || branch.equals(pr.headBranch()))
                .limit(limit)
                .toList();
    }

    private MySpaceAnalysisDetailResponse detail(Analysis analysis) {
        Finding finding = findingRepository.findByAnalysisId(analysis.getId()).orElse(null);
        List<MySpaceAnalysisDetailResponse.Issue> security = issues(
                finding == null ? null : finding.getSecuResult(), "SECURITY");
        List<MySpaceAnalysisDetailResponse.Issue> inefficiency = issues(
                finding == null ? null : finding.getInefficiencyResult(), "INEFFICIENCY");
        List<MySpaceAnalysisDetailResponse.Issue> other = issues(
                finding == null ? null : finding.getDuplicateResult(), "OTHER");
        return new MySpaceAnalysisDetailResponse(
                analysis.getId(),
                analysis.getGithubRepo().getId(),
                analysis.getGithubRepo().getName(),
                analysis.getBranch(),
                analysis.getFilePath(),
                analysis.getLanguage(),
                analysis.getStatus(),
                analysis.getCreatedAt(),
                totalIssues(finding),
                securityIssues(finding),
                inefficiencyIssues(finding),
                otherIssues(finding),
                qualityScore(finding),
                analysis.getImprovableRatio(),
                security,
                inefficiency,
                other,
                analysis.getOriginCode(),
                finding == null ? null : finding.getModifiedCode(),
                finding != null && finding.isAiGenerated(),
                finding == null ? null : finding.getAiProbability());
    }

    private List<MySpaceAnalysisDetailResponse.Issue> issues(String json, String type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> values = jsonMapper.readValue(
                    json, new TypeReference<List<Map<String, Object>>>() {});
            return values.stream().map(value -> issue(value, type)).toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private MySpaceAnalysisDetailResponse.Issue issue(Map<String, Object> value, String type) {
        Integer score = integer(value.get("complexity_score"));
        String message = string(value.get("message"));
        return new MySpaceAnalysisDetailResponse.Issue(
                type,
                severity(type, score),
                integer(value.getOrDefault("line", value.get("start_line"))),
                string(value.get("rule_id")),
                string(value.get("function_name")),
                message == null && "OTHER".equals(type) ? "유사 코드가 발견되었습니다." : message,
                message == null && "OTHER".equals(type)
                        ? "중복 로직을 공통 함수로 추출하세요."
                        : message,
                issueScore(score, value.get("similarity_score")));
    }

    private String severity(String type, Integer score) {
        if ("SECURITY".equals(type)) {
            return "HIGH";
        }
        if ("OTHER".equals(type)) {
            return "MEDIUM";
        }
        if (score == null || score < 10) {
            return "LOW";
        }
        if (score >= 25) {
            return "CRITICAL";
        }
        return score >= 15 ? "HIGH" : "MEDIUM";
    }

    private Integer integer(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private Double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private Double issueScore(Integer complexityScore, Object similarityScore) {
        return complexityScore == null ? number(similarityScore) : Double.valueOf(complexityScore);
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private UserRepo getConnectedRepo(Integer userId, Integer repoId) {
        return userRepoRepository.findByUser_IdAndGithubRepo_Id(userId, repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));
    }

    private void validateBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_BRANCH);
        }
    }

    private MySpaceSummaryResponse emptySummary(GithubRepo repo, String branch, long totalAnalysisCount) {
        return new MySpaceSummaryResponse(
                repo.getId(), repo.getName(), branch,
                null, null, null,
                totalAnalysisCount,
                0, 0, 0, 0,
                null, 0,
                new MySpaceSummaryResponse.Comparison(null, null, null, null, null),
                null, null, null, null);
    }

    private Map<Integer, Finding> findingsByAnalysisId(List<Analysis> analyses) {
        return findingRepository.findAllByAnalysisIdIn(
                        analyses.stream().map(Analysis::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        finding -> finding.getAnalysis().getId(),
                        Function.identity(),
                        (first, ignored) -> first));
    }

    private Aggregate aggregate(List<Analysis> analyses, Map<Integer, Finding> findings) {
        List<Finding> availableFindings = analyses.stream()
                .map(analysis -> findings.get(analysis.getId()))
                .filter(java.util.Objects::nonNull)
                .toList();
        int total = availableFindings.stream().mapToInt(this::totalIssues).sum();
        int security = availableFindings.stream().mapToInt(this::securityIssues).sum();
        int inefficiency = availableFindings.stream().mapToInt(this::inefficiencyIssues).sum();
        int other = availableFindings.stream().mapToInt(this::otherIssues).sum();
        double quality = availableFindings.stream()
                .mapToDouble(this::qualityScore)
                .average()
                .orElse(0);
        List<BigDecimal> ratios = analyses.stream()
                .map(Analysis::getImprovableRatio)
                .filter(java.util.Objects::nonNull)
                .toList();
        BigDecimal averageRatio = ratios.isEmpty() ? null : ratios.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(ratios.size()), 1, RoundingMode.HALF_UP);
        return new Aggregate(total, security, inefficiency, other,
                averageRatio, roundOneDecimal(quality));
    }

    private MySpaceSummaryResponse.Comparison comparison(
            Aggregate current,
            Aggregate previous,
            boolean noPreviousAnalysis
    ) {
        if (noPreviousAnalysis) {
            return new MySpaceSummaryResponse.Comparison(null, null, null, null, null);
        }
        return new MySpaceSummaryResponse.Comparison(
                current.totalIssues() - previous.totalIssues(),
                current.securityIssues() - previous.securityIssues(),
                current.inefficiencyIssues() - previous.inefficiencyIssues(),
                current.improvableRatio() == null || previous.improvableRatio() == null
                        ? null
                        : current.improvableRatio().subtract(previous.improvableRatio()),
                roundOneDecimal(current.qualityScore() - previous.qualityScore()));
    }

    private double changeRate(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0 : 100;
        }
        return roundOneDecimal((current - previous) * 100.0 / previous);
    }

    private int totalIssues(Finding finding) {
        return finding == null || finding.getTotalIssues() == null ? 0 : finding.getTotalIssues();
    }

    private int securityIssues(Finding finding) {
        return finding == null || finding.getSecurityCount() == null ? 0 : finding.getSecurityCount();
    }

    private int inefficiencyIssues(Finding finding) {
        return finding == null || finding.getInefficiencyCount() == null
                ? 0 : finding.getInefficiencyCount();
    }

    private int otherIssues(Finding finding) {
        return Math.max(0, totalIssues(finding) - securityIssues(finding) - inefficiencyIssues(finding));
    }

    private double qualityScore(Finding finding) {
        if (finding == null) {
            return 0;
        }
        return Math.max(0,
                100
                        - securityIssues(finding) * 10
                        - inefficiencyIssues(finding) * 5
                        - otherIssues(finding) * 3);
    }

    private double roundOneDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private record Aggregate(
            int totalIssues,
            int securityIssues,
            int inefficiencyIssues,
            int otherIssues,
            BigDecimal improvableRatio,
            double qualityScore
    ) {
    }
}
