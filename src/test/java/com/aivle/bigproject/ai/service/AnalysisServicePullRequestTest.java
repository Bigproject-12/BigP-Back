package com.aivle.bigproject.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.bigproject.dto.repo.GithubPullRequestResult;
import com.aivle.bigproject.dto.repo.GithubFileContent;
import com.aivle.bigproject.dto.analysis.BatchPushRequest;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Company;
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
import java.util.List;
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

        String url = analysisService.createPullRequest(20, 1, null, null, null);

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

    @Test
    void pushesSingleImprovedFileUsingLatestFileSha() {
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
                .branch("dev")
                .filePath("src/App.java")
                .build();
        Finding finding = Finding.builder().modifiedCode("class App {}").build();

        when(analysisRepository.findById(20)).thenReturn(Optional.of(analysis));
        when(findingRepository.findByAnalysisId(20)).thenReturn(Optional.of(finding));
        when(githubService.getFileSha(1, "aivle", "BigP-Back", "src/App.java", "dev"))
                .thenReturn("file-sha");

        analysisService.pushImprovedCode(20, 1);

        verify(githubService).commitFile(
                1, "aivle", "BigP-Back", "src/App.java", "dev",
                "class App {}", "file-sha", "GuardrAil: AI 코드 개선 반영 (분석 ID: 20)");
    }

    @Test
    void preparesBatchPushAfterValidatingSelectedAnalyses() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder()
                .id(10)
                .organization("aivle")
                .name("BigP-Back")
                .build();
        Analysis first = completedAnalysis(20, user, repo, "dev", "src/A.java");
        Analysis second = completedAnalysis(21, user, repo, "dev", "src/B.java");

        when(analysisRepository.findAllById(List.of(20, 21)))
                .thenReturn(List.of(first, second));
        when(findingRepository.findByAnalysisId(20))
                .thenReturn(Optional.of(Finding.builder().modifiedCode("class A {}").build()));
        when(findingRepository.findByAnalysisId(21))
                .thenReturn(Optional.of(Finding.builder().modifiedCode("class B {}").build()));
        when(githubService.getBranchHeadSha(1, "aivle", "BigP-Back", "dev"))
                .thenReturn("head-sha");

        var response = analysisService.prepareBatchPush(
                new BatchPushRequest(List.of(20, 21), "main", "title", "body"), 1);

        assertEquals(List.of(20, 21), response.analysisIds());
        assertEquals("aivle/BigP-Back", response.repository());
        assertEquals("dev", response.branch());
        assertEquals("head-sha", response.branchHeadSha());
        assertEquals("READY", response.status());
    }

    @Test
    void rejectsBatchPushWhenBranchesDiffer() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder().id(10).build();
        Analysis first = completedAnalysis(20, user, repo, "dev", "src/A.java");
        Analysis second = completedAnalysis(21, user, repo, "main", "src/B.java");

        when(analysisRepository.findAllById(List.of(20, 21)))
                .thenReturn(List.of(first, second));
        when(findingRepository.findByAnalysisId(20))
                .thenReturn(Optional.of(Finding.builder().modifiedCode("class A {}").build()));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.prepareBatchPush(
                        new BatchPushRequest(List.of(20, 21), "main", null, null), 1));

        assertEquals(ErrorCode.BATCH_BRANCH_MISMATCH, exception.getErrorCode());
        verify(githubService, never()).getBranchHeadSha(any(), any(), any(), any());
    }

    private Analysis completedAnalysis(
            Integer id, User user, GithubRepo repo, String branch, String filePath) {
        return Analysis.builder()
                .id(id)
                .user(user)
                .githubRepo(repo)
                .status("COMPLETED")
                .branch(branch)
                .filePath(filePath)
                .build();
    }

    @Test
    void preparesReanalysisWithLatestGithubCodeAndNewAnalysis() {
        User user = User.builder().id(1).build();
        Company company = Company.builder().id(2).name("AIVLE").build();
        GithubRepo repo = GithubRepo.builder().id(10).name("BigP-Back").build();
        Analysis previous = Analysis.builder()
                .id(20)
                .user(user)
                .company(company)
                .githubRepo(repo)
                .status("COMPLETED")
                .branch("dev")
                .filePath("src/App.java")
                .language("Java")
                .originCode("class App { /* old */ }")
                .prompt("review")
                .build();
        when(analysisRepository.findById(20)).thenReturn(Optional.of(previous));
        when(githubService.getLatestFileContent(1, 10, "src/App.java", "dev"))
                .thenReturn(new GithubFileContent("class App { /* latest */ }", "latest-sha"));
        when(analysisRepository.save(any(Analysis.class))).thenAnswer(invocation -> {
            Analysis saved = invocation.getArgument(0);
            saved.setId(21);
            return saved;
        });

        var response = analysisService.prepareReanalysis(20, 1);

        assertEquals(21, response.analysisId());
        assertEquals("class App { /* latest */ }", response.request().codeContent());
        ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        verify(analysisRepository).save(analysisCaptor.capture());
        assertEquals("ANALYZING", analysisCaptor.getValue().getStatus());
        assertEquals("latest-sha", analysisCaptor.getValue().getSourceBlobSha());
        assertEquals(20, previous.getId());
    }

    @Test
    void rejectsReanalysisWhenGithubCodeHasNotChanged() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder().id(10).build();
        Analysis previous = Analysis.builder()
                .id(20)
                .user(user)
                .githubRepo(repo)
                .status("COMPLETED")
                .branch("dev")
                .filePath("src/App.java")
                .language("Java")
                .originCode("class App {}")
                .sourceBlobSha("same-sha")
                .build();
        when(analysisRepository.findById(20)).thenReturn(Optional.of(previous));
        when(githubService.getLatestFileContent(1, 10, "src/App.java", "dev"))
                .thenReturn(new GithubFileContent("class App {}", "same-sha"));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.prepareReanalysis(20, 1));

        assertEquals(ErrorCode.SOURCE_NOT_CHANGED, exception.getErrorCode());
        verify(analysisRepository, never()).save(any(Analysis.class));
    }
}
