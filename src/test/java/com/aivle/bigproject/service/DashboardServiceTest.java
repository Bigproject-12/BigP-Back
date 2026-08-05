package com.aivle.bigproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Company;
import com.aivle.bigproject.entity.Finding;
import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.GithubPullRequest;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.FindingRepository;
import com.aivle.bigproject.repository.GithubPullRequestRepository;
import com.aivle.bigproject.repository.UserRepository;
import com.aivle.bigproject.repository.UserRepoRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock AnalysisRepository analysisRepository;
    @Mock FindingRepository findingRepository;
    @Mock UserRepository userRepository;
    @Mock GithubPullRequestRepository githubPullRequestRepository;
    @Mock UserRepoRepository userRepoRepository;
    @InjectMocks DashboardService dashboardService;

    @Test
    void returnsCompanyWideDashboardSummary() {
        Company company = Company.builder().id(100).name("AIVLE").build();
        User user = User.builder().id(1).company(company).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        GithubRepo repo = GithubRepo.builder().id(10).name("BigP-Back").build();
        Analysis analysis = Analysis.builder()
                .id(20)
                .githubRepo(repo)
                .language("Java")
                .status("COMPLETED")
                .build();
        Finding finding = Finding.builder().totalIssues(4).build();
        GithubPullRequest pullRequest = GithubPullRequest.builder()
                .id(30)
                .githubRepo(repo)
                .githubPrNumber(42)
                .title("AI 코드 개선")
                .status("OPEN")
                .prUrl("https://github.com/aivle/BigP-Back/pull/42")
                .headBranch("feature/fix")
                .baseBranch("main")
                .createdAt(LocalDateTime.of(2026, 7, 24, 11, 0))
                .build();
        FindingRepository.IssueCountSummary issueCounts =
                mock(FindingRepository.IssueCountSummary.class);
        FindingRepository.IssueCountSummary previousIssueCounts =
                mock(FindingRepository.IssueCountSummary.class);
        FindingRepository.DailyQualityScore dailyQualityScore =
                mock(FindingRepository.DailyQualityScore.class);
        FindingRepository.RiskRepositorySummary riskRepository =
                mock(FindingRepository.RiskRepositorySummary.class);

        when(userRepoRepository.countDistinctRepoByCompanyId(100)).thenReturn(2L);
        when(analysisRepository.countByCompanyIdAndPeriod(
                anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(3L, 2L);
        when(analysisRepository.countByCompanyIdAndStatusAndPeriod(
                anyInt(), anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> "COMPLETED".equals(invocation.getArgument(1)) ? 2L : 0L);
        when(issueCounts.getTotalIssueCount()).thenReturn(7L);
        when(issueCounts.getSecurityIssueCount()).thenReturn(3L);
        when(issueCounts.getInefficiencyIssueCount()).thenReturn(2L);
        when(previousIssueCounts.getTotalIssueCount()).thenReturn(10L);
        when(findingRepository.sumIssueCountsByCompanyIdAndPeriod(
                anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(issueCounts, previousIssueCounts);
        when(findingRepository.averageQualityScoreByCompanyIdAndPeriod(
                anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(80.0, 70.0);
        when(dailyQualityScore.getAnalysisDate()).thenReturn(LocalDate.of(2026, 7, 24));
        when(dailyQualityScore.getAverageScore()).thenReturn(80.0);
        when(findingRepository.findDailyQualityScoresByCompanyIdAndPeriod(
                anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(dailyQualityScore));
        when(riskRepository.getRepoId()).thenReturn(10);
        when(riskRepository.getRepoName()).thenReturn("BigP-Back");
        when(riskRepository.getQualityScore()).thenReturn(64.0);
        when(riskRepository.getTotalIssueCount()).thenReturn(7L);
        when(riskRepository.getSecurityIssueCount()).thenReturn(3L);
        when(riskRepository.getInefficiencyIssueCount()).thenReturn(2L);
        when(riskRepository.getOtherIssueCount()).thenReturn(2L);
        when(riskRepository.getLastAnalyzedAt())
                .thenReturn(LocalDateTime.of(2026, 7, 24, 10, 30));
        when(findingRepository.findTop5RiskRepositoriesByCompanyIdAndPeriod(
                anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(riskRepository));
        when(analysisRepository.findTop7ByCompanyIdAndPeriod(
                anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(analysis));
        when(findingRepository.findByAnalysisId(20)).thenReturn(Optional.of(finding));
        when(githubPullRequestRepository.countByCompanyIdAndPeriod(
                anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(4L, 2L);
        when(githubPullRequestRepository.countByCompanyIdAndStatusAndPeriod(
                anyInt(), anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> switch ((String) invocation.getArgument(1)) {
                    case "OPEN" -> 2L;
                    case "MERGED", "CLOSED" -> 1L;
                    default -> 0L;
                });
        when(githubPullRequestRepository
                .findRecentByCompanyIdAndPeriod(
                        anyInt(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(pullRequest));

        var response = dashboardService.getDashboard(
                1, LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 24));

        assertEquals(2, response.repositoryCount());
        assertEquals(3, response.totalAnalysisCount());
        assertEquals(7, response.totalIssueCount());
        assertEquals(80.0, response.averageQualityScore());
        assertEquals(50.0, response.comparison().analysisChangeRate());
        assertEquals(-30.0, response.comparison().issueChangeRate());
        assertEquals(10.0, response.comparison().qualityScoreChange());
        assertEquals(4, response.totalPullRequestCount());
        assertEquals(2, response.openPullRequestCount());
        assertEquals(1, response.mergedPullRequestCount());
        assertEquals(1, response.closedPullRequestCount());
        assertEquals(100.0, response.comparison().pullRequestChangeRate());
        assertEquals(7, response.qualityScoreTrend().size());
        assertEquals(80.0, response.qualityScoreTrend().get(6).averageScore());
        assertEquals(3, response.issueDistribution().size());
        assertEquals(42.9, response.issueDistribution().get(0).percentage());
        assertEquals(28.6, response.issueDistribution().get(2).percentage());
        assertEquals(1, response.riskRepositories().size());
        assertEquals(1, response.riskRepositories().get(0).rank());
        assertEquals("BigP-Back", response.riskRepositories().get(0).repoName());
        assertEquals(64.0, response.riskRepositories().get(0).qualityScore());
        assertEquals(2, response.riskRepositories().get(0).otherIssueCount());
        assertEquals("BigP-Back", response.recentAnalyses().get(0).repoName());
        assertEquals(4, response.recentAnalyses().get(0).totalIssueCount());
        assertEquals(42, response.recentPullRequests().get(0).githubPrNumber());
        assertEquals("https://github.com/aivle/BigP-Back/pull/42",
                response.recentPullRequests().get(0).prUrl());
    }

    @Test
    void rejectsReversedDateRange() {
        assertThrows(CustomException.class, () -> dashboardService.getDashboard(
                1, LocalDate.of(2026, 7, 25), LocalDate.of(2026, 7, 24)));
    }
}
