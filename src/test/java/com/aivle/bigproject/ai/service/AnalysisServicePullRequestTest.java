package com.aivle.bigproject.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.bigproject.dto.repo.GithubPullRequestResult;
import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Finding;
import com.aivle.bigproject.entity.GithubPullRequest;
import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.PullRequestAnalysis;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.CompanyRepository;
import com.aivle.bigproject.repository.FindingRepository;
import com.aivle.bigproject.repository.GithubPullRequestRepository;
import com.aivle.bigproject.repository.GithubRepoRepository;
import com.aivle.bigproject.repository.PullRequestAnalysisRepository;
import com.aivle.bigproject.repository.UserRepository;
import com.aivle.bigproject.service.GithubService;
import com.aivle.bigproject.service.NotificationService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class AnalysisServicePullRequestTest {

    @Mock AnalysisRepository analysisRepository;
    @Mock GithubRepoRepository githubRepoRepository;
    @Mock CompanyRepository companyRepository;
    @Mock UserRepository userRepository;
    @Mock FindingRepository findingRepository;
    @Mock NotificationService notificationService;
    @Mock EmbeddingService embeddingService;
    @Mock JsonMapper jsonMapper;
    @Mock GithubService githubService;
    @Mock GithubPullRequestRepository githubPullRequestRepository;
    @Mock PullRequestAnalysisRepository pullRequestAnalysisRepository;
    @InjectMocks AnalysisService analysisService;

    @Test
    void createsAndStoresPullRequestThenReturnsGithubUrl() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder()
                .id(10)
                .organization("aivle")
                .name("BigP-Back")
                .build();
        Analysis analysis = Analysis.builder()
                .id(20)
                .user(user)
                .githubRepo(repo)
                .status("COMPLETED")
                .branch("feature/guardrail-fix")
                .filePath("src/App.java")
                .build();
        Finding finding = Finding.builder()
                .totalIssues(3)
                .securityCount(1)
                .inefficiencyCount(2)
                .build();
        GithubPullRequestResult result = new GithubPullRequestResult(
                42,
                "https://github.com/aivle/BigP-Back/pull/42",
                "OPEN",
                false,
                "abc123"
        );

        when(analysisRepository.findById(20)).thenReturn(Optional.of(analysis));
        when(pullRequestAnalysisRepository.existsByAnalysis_Id(20)).thenReturn(false);
        when(findingRepository.findByAnalysisId(20)).thenReturn(Optional.of(finding));
        when(githubService.getDefaultBranch(1, "aivle", "BigP-Back")).thenReturn("main");
        when(githubService.createPullRequest(
                any(), any(), any(), any(), any(), any(), any())).thenReturn(result);
        when(githubPullRequestRepository.save(any(GithubPullRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String url = analysisService.createPullRequest(20, 1, null);

        assertEquals(result.url(), url);
        ArgumentCaptor<GithubPullRequest> prCaptor =
                ArgumentCaptor.forClass(GithubPullRequest.class);
        verify(githubPullRequestRepository).save(prCaptor.capture());
        assertEquals(42, prCaptor.getValue().getGithubPrNumber());
        assertEquals("OPEN", prCaptor.getValue().getStatus());

        ArgumentCaptor<PullRequestAnalysis> linkCaptor =
                ArgumentCaptor.forClass(PullRequestAnalysis.class);
        verify(pullRequestAnalysisRepository).save(linkCaptor.capture());
        assertEquals(analysis, linkCaptor.getValue().getAnalysis());
        assertEquals(prCaptor.getValue(), linkCaptor.getValue().getPullRequest());
    }
}
