package com.aivle.bigproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Finding;
import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.FindingRepository;
import com.aivle.bigproject.repository.UserRepoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock AnalysisRepository analysisRepository;
    @Mock FindingRepository findingRepository;
    @Mock UserRepoRepository userRepoRepository;
    @InjectMocks DashboardService dashboardService;

    @Test
    void returnsOnlyUsersDashboardSummary() {
        GithubRepo repo = GithubRepo.builder().id(10).name("BigP-Back").build();
        Analysis analysis = Analysis.builder()
                .id(20)
                .githubRepo(repo)
                .language("Java")
                .status("COMPLETED")
                .build();
        Finding finding = Finding.builder().totalIssues(4).build();
        FindingRepository.IssueCountSummary issueCounts =
                mock(FindingRepository.IssueCountSummary.class);

        when(userRepoRepository.countByUserId(1)).thenReturn(2L);
        when(analysisRepository.countByUserId(1)).thenReturn(3L);
        when(analysisRepository.countByUserIdAndStatus(anyInt(), anyString()))
                .thenAnswer(invocation -> "COMPLETED".equals(invocation.getArgument(1)) ? 2L : 0L);
        when(issueCounts.getTotalIssueCount()).thenReturn(7L);
        when(issueCounts.getSecurityIssueCount()).thenReturn(3L);
        when(issueCounts.getInefficiencyIssueCount()).thenReturn(4L);
        when(findingRepository.sumIssueCountsByUserId(1)).thenReturn(issueCounts);
        when(analysisRepository.findTop5ByUserIdOrderByIdDesc(1)).thenReturn(List.of(analysis));
        when(findingRepository.findByAnalysisId(20)).thenReturn(Optional.of(finding));

        var response = dashboardService.getDashboard(1);

        assertEquals(2, response.repositoryCount());
        assertEquals(3, response.totalAnalysisCount());
        assertEquals(7, response.totalIssueCount());
        assertEquals("BigP-Back", response.recentAnalyses().get(0).repoName());
        assertEquals(4, response.recentAnalyses().get(0).totalIssueCount());
    }
}
