package com.aivle.bigproject.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.bigproject.dto.repo.GithubPullRequestResult;
import com.aivle.bigproject.dto.repo.GithubFileContent;
import com.aivle.bigproject.dto.analysis.BatchPushRequest;
import com.aivle.bigproject.dto.analysis.BatchPullRequestRequest;
import com.aivle.bigproject.dto.repo.GithubTreeItem;
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
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.StreamSupport;
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
    void routesSinglePushThroughSafeBatchWorkflow() {
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
                .originCode("original")
                .sourceBlobSha("source-sha-20")
                .build();
        Finding finding = Finding.builder().modifiedCode("class App {}").build();

        when(analysisRepository.findAllById(List.of(20))).thenReturn(List.of(analysis));
        when(findingRepository.findByAnalysisId(20)).thenReturn(Optional.of(finding));
        when(githubService.getBranchHeadSha(1, "aivle", "BigP-Back", "dev"))
                .thenReturn("head-sha");
        when(githubService.getLatestFileContent(1, 10, "src/App.java", "head-sha"))
                .thenReturn(new GithubFileContent("original", "source-sha-20"));
        when(githubService.getCommitTreeSha(1, "aivle", "BigP-Back", "head-sha"))
                .thenReturn("base-tree-sha");
        when(githubService.getFileModes(
                1, "aivle", "BigP-Back", "base-tree-sha", Set.of("src/App.java")))
                .thenReturn(Map.of("src/App.java", "100644"));
        when(githubService.createBlob(1, "aivle", "BigP-Back", "class App {}"))
                .thenReturn("blob-sha");
        when(githubService.createTree(
                1, "aivle", "BigP-Back", "base-tree-sha",
                List.of(new GithubTreeItem("src/App.java", "blob-sha", "100644"))))
                .thenReturn("tree-sha");
        when(githubService.createCommit(
                1, "aivle", "BigP-Back", "GuardrAil: AI 코드 개선 반영 (1개 파일)",
                "tree-sha", "head-sha"))
                .thenReturn("commit-sha");

        analysisService.pushImprovedCode(20, 1);

        verify(githubService).updateBranchHead(
                1, "aivle", "BigP-Back", "dev", "commit-sha");
        verify(analysisRepository).saveAll(List.of(analysis));
    }

    @Test
    void pushesSelectedAnalysesAsOneCommit() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder()
                .id(10)
                .organization("aivle")
                .name("BigP-Back")
                .build();
        Analysis first = completedAnalysis(20, user, repo, "dev", "src/A.java");
        Analysis second = completedAnalysis(21, user, repo, "dev", "src/B.java");
        stubLatestFiles(List.of(first, second));

        when(analysisRepository.findAllById(List.of(20, 21)))
                .thenReturn(List.of(first, second));
        when(findingRepository.findByAnalysisId(20))
                .thenReturn(Optional.of(Finding.builder().modifiedCode("class A {}").build()));
        when(findingRepository.findByAnalysisId(21))
                .thenReturn(Optional.of(Finding.builder().modifiedCode("class B {}").build()));
        when(githubService.getBranchHeadSha(1, "aivle", "BigP-Back", "dev"))
                .thenReturn("head-sha");
        when(githubService.getCommitTreeSha(1, "aivle", "BigP-Back", "head-sha"))
                .thenReturn("base-tree-sha");
        when(githubService.createBlob(1, "aivle", "BigP-Back", "class A {}"))
                .thenReturn("blob-a");
        when(githubService.createBlob(1, "aivle", "BigP-Back", "class B {}"))
                .thenReturn("blob-b");
        when(githubService.createTree(
                1,
                "aivle",
                "BigP-Back",
                "base-tree-sha",
                List.of(
                        new GithubTreeItem("src/A.java", "blob-a", "100644"),
                        new GithubTreeItem("src/B.java", "blob-b", "100755"))))
                .thenReturn("prepared-tree-sha");
        when(githubService.createCommit(
                1,
                "aivle",
                "BigP-Back",
                "GuardrAil: AI 코드 개선 반영 (2개 파일)",
                "prepared-tree-sha",
                "head-sha"))
                .thenReturn("commit-sha");

        var response = analysisService.batchPush(
                new BatchPushRequest(List.of(20, 21)), 1);

        assertEquals(List.of(20, 21), response.analysisIds());
        assertEquals("aivle/BigP-Back", response.repository());
        assertEquals("dev", response.branch());
        assertEquals("head-sha", response.branchHeadSha());
        assertEquals("base-tree-sha", response.baseTreeSha());
        assertEquals("prepared-tree-sha", response.treeSha());
        assertEquals("commit-sha", response.commitSha());
        assertEquals(List.of("blob-a", "blob-b"),
                response.files().stream().map(file -> file.blobSha()).toList());
        assertEquals("PUSHED", response.status());
        assertEquals("commit-sha", first.getPushedCommitSha());
        assertEquals(response.pushedAt(), first.getPushedAt());
        assertEquals("commit-sha", second.getPushedCommitSha());
        verify(githubService).updateBranchHead(
                1, "aivle", "BigP-Back", "dev", "commit-sha");
        verify(analysisRepository).saveAll(List.of(first, second));
    }

    @Test
    void doesNotCreateTreeWhenAnyBlobCreationFails() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder()
                .id(10)
                .organization("aivle")
                .name("BigP-Back")
                .build();
        Analysis first = completedAnalysis(20, user, repo, "dev", "src/A.java");
        Analysis second = completedAnalysis(21, user, repo, "dev", "src/B.java");
        stubLatestFiles(List.of(first, second));

        when(analysisRepository.findAllById(List.of(20, 21)))
                .thenReturn(List.of(first, second));
        when(findingRepository.findByAnalysisId(20))
                .thenReturn(Optional.of(Finding.builder().modifiedCode("class A {}").build()));
        when(findingRepository.findByAnalysisId(21))
                .thenReturn(Optional.of(Finding.builder().modifiedCode("class B {}").build()));
        when(githubService.getBranchHeadSha(1, "aivle", "BigP-Back", "dev"))
                .thenReturn("head-sha");
        when(githubService.getCommitTreeSha(1, "aivle", "BigP-Back", "head-sha"))
                .thenReturn("base-tree-sha");
        when(githubService.createBlob(1, "aivle", "BigP-Back", "class A {}"))
                .thenReturn("blob-a");
        when(githubService.createBlob(1, "aivle", "BigP-Back", "class B {}"))
                .thenThrow(new CustomException(ErrorCode.GITHUB_BLOB_CREATE_FAILED));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.batchPush(
                        new BatchPushRequest(List.of(20, 21)), 1));

        assertEquals(ErrorCode.GITHUB_BLOB_CREATE_FAILED, exception.getErrorCode());
        verify(githubService, never()).createTree(any(), any(), any(), any(), any());
    }

    @Test
    void doesNotSavePushResultWhenBranchUpdateFails() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder()
                .id(10).organization("aivle").name("BigP-Back").build();
        Analysis analysis = completedAnalysis(20, user, repo, "dev", "src/A.java");
        stubLatestFiles(List.of(analysis));

        when(analysisRepository.findAllById(List.of(20))).thenReturn(List.of(analysis));
        when(findingRepository.findByAnalysisId(20))
                .thenReturn(Optional.of(Finding.builder().modifiedCode("class A {}").build()));
        when(githubService.getBranchHeadSha(1, "aivle", "BigP-Back", "dev"))
                .thenReturn("head-sha");
        when(githubService.getCommitTreeSha(1, "aivle", "BigP-Back", "head-sha"))
                .thenReturn("base-tree-sha");
        when(githubService.createBlob(1, "aivle", "BigP-Back", "class A {}"))
                .thenReturn("blob-a");
        when(githubService.createTree(
                1, "aivle", "BigP-Back", "base-tree-sha",
                List.of(new GithubTreeItem("src/A.java", "blob-a", "100644"))))
                .thenReturn("tree-sha");
        when(githubService.createCommit(
                1, "aivle", "BigP-Back",
                "GuardrAil: AI 코드 개선 반영 (1개 파일)", "tree-sha", "head-sha"))
                .thenReturn("commit-sha");
        doThrow(new CustomException(ErrorCode.GITHUB_BRANCH_UPDATE_FAILED))
                .when(githubService)
                .updateBranchHead(1, "aivle", "BigP-Back", "dev", "commit-sha");

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.batchPush(
                        new BatchPushRequest(List.of(20)), 1));

        assertEquals(ErrorCode.GITHUB_BRANCH_UPDATE_FAILED, exception.getErrorCode());
        assertNull(analysis.getPushedCommitSha());
        verify(analysisRepository, never()).saveAll(any());
    }

    @Test
    void rejectsAnalysisThatWasAlreadyPushed() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder().id(10).build();
        Analysis analysis = completedAnalysis(20, user, repo, "dev", "src/A.java");
        analysis.markPushed("existing-commit", java.time.LocalDateTime.now());
        when(analysisRepository.findAllById(List.of(20))).thenReturn(List.of(analysis));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.batchPush(
                        new BatchPushRequest(List.of(20)), 1));

        assertEquals(ErrorCode.ANALYSIS_ALREADY_PUSHED, exception.getErrorCode());
        verify(githubService, never()).getBranchHeadSha(any(), any(), any(), any());
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
                () -> analysisService.batchPush(
                        new BatchPushRequest(List.of(20, 21)), 1));

        assertEquals(ErrorCode.BATCH_BRANCH_MISMATCH, exception.getErrorCode());
        verify(githubService, never()).getBranchHeadSha(any(), any(), any(), any());
    }

    @Test
    void rejectsBatchPushOwnedByAnotherUser() {
        User owner = User.builder().id(2).build();
        GithubRepo repo = GithubRepo.builder().id(10).build();
        Analysis analysis = completedAnalysis(20, owner, repo, "dev", "src/A.java");
        when(analysisRepository.findAllById(List.of(20))).thenReturn(List.of(analysis));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.batchPush(new BatchPushRequest(List.of(20)), 1));

        assertEquals(ErrorCode.NO_PERMISSION, exception.getErrorCode());
        verify(githubService, never()).getLatestFileContent(any(), any(), any(), any());
    }

    @Test
    void rejectsDuplicatedAnalysisIds() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.batchPush(new BatchPushRequest(List.of(20, 20)), 1));

        assertEquals(ErrorCode.BATCH_ANALYSIS_DUPLICATED, exception.getErrorCode());
        verify(analysisRepository, never()).findAllById(any());
    }

    @Test
    void rejectsBatchPushAcrossRepositories() {
        User user = User.builder().id(1).build();
        GithubRepo firstRepo = GithubRepo.builder().id(10).build();
        GithubRepo secondRepo = GithubRepo.builder().id(11).build();
        Analysis first = completedAnalysis(20, user, firstRepo, "dev", "src/A.java");
        Analysis second = completedAnalysis(21, user, secondRepo, "dev", "src/B.java");
        when(analysisRepository.findAllById(List.of(20, 21)))
                .thenReturn(List.of(first, second));
        when(findingRepository.findByAnalysisId(20))
                .thenReturn(Optional.of(Finding.builder().modifiedCode("class A {}").build()));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.batchPush(new BatchPushRequest(List.of(20, 21)), 1));

        assertEquals(ErrorCode.BATCH_REPOSITORY_MISMATCH, exception.getErrorCode());
    }

    @Test
    void rejectsDuplicatedFilePaths() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder().id(10).build();
        Analysis first = completedAnalysis(20, user, repo, "dev", "src/A.java");
        Analysis second = completedAnalysis(21, user, repo, "dev", "src/A.java");
        when(analysisRepository.findAllById(List.of(20, 21)))
                .thenReturn(List.of(first, second));
        when(findingRepository.findByAnalysisId(20))
                .thenReturn(Optional.of(Finding.builder().modifiedCode("class A {}").build()));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.batchPush(new BatchPushRequest(List.of(20, 21)), 1));

        assertEquals(ErrorCode.BATCH_FILE_DUPLICATED, exception.getErrorCode());
    }

    @Test
    void rejectsMissingImprovedCode() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder().id(10).build();
        Analysis analysis = completedAnalysis(20, user, repo, "dev", "src/A.java");
        when(analysisRepository.findAllById(List.of(20))).thenReturn(List.of(analysis));
        when(findingRepository.findByAnalysisId(20))
                .thenReturn(Optional.of(Finding.builder().modifiedCode(" ").build()));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.batchPush(new BatchPushRequest(List.of(20)), 1));

        assertEquals(ErrorCode.IMPROVED_CODE_MISSING, exception.getErrorCode());
    }

    @Test
    void rejectsFileChangedAfterAnalysisBeforeCreatingBlob() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder()
                .id(10).organization("aivle").name("BigP-Back").build();
        Analysis analysis = completedAnalysis(20, user, repo, "dev", "src/A.java");
        when(analysisRepository.findAllById(List.of(20))).thenReturn(List.of(analysis));
        when(findingRepository.findByAnalysisId(20))
                .thenReturn(Optional.of(Finding.builder().modifiedCode("class A {}").build()));
        when(githubService.getBranchHeadSha(1, "aivle", "BigP-Back", "dev"))
                .thenReturn("head-sha");
        when(githubService.getLatestFileContent(1, 10, "src/A.java", "head-sha"))
                .thenReturn(new GithubFileContent("changed", "different-sha"));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.batchPush(new BatchPushRequest(List.of(20)), 1));

        assertEquals(ErrorCode.SOURCE_CHANGED_SINCE_ANALYSIS, exception.getErrorCode());
        verify(githubService, never()).createBlob(any(), any(), any(), any());
    }

    @Test
    void recoversPushTrackingWhenImprovedCodeIsAlreadyOnGithub() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder()
                .id(10).organization("aivle").name("BigP-Back").build();
        Analysis analysis = completedAnalysis(20, user, repo, "dev", "src/A.java");
        when(analysisRepository.findAllById(List.of(20))).thenReturn(List.of(analysis));
        when(findingRepository.findByAnalysisId(20))
                .thenReturn(Optional.of(Finding.builder().modifiedCode("class A {}").build()));
        when(githubService.getBranchHeadSha(1, "aivle", "BigP-Back", "dev"))
                .thenReturn("recovered-head-sha");
        when(githubService.getLatestFileContent(1, 10, "src/A.java", "recovered-head-sha"))
                .thenReturn(new GithubFileContent("class A {}", "new-file-sha"));

        var response = analysisService.batchPush(new BatchPushRequest(List.of(20)), 1);

        assertEquals("RECOVERED", response.status());
        assertEquals("recovered-head-sha", response.commitSha());
        assertEquals("recovered-head-sha", analysis.getPushedCommitSha());
        verify(analysisRepository).saveAll(List.of(analysis));
        verify(githubService, never()).createBlob(any(), any(), any(), any());
        verify(githubService, never()).updateBranchHead(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsPullRequestForDifferentPushCommits() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder().id(10).build();
        Analysis first = pushedAnalysis(20, user, repo, "dev", "src/A.java", "commit-a");
        Analysis second = pushedAnalysis(21, user, repo, "dev", "src/B.java", "commit-b");
        when(analysisRepository.findAllById(List.of(20, 21)))
                .thenReturn(List.of(first, second));
        when(pullRequestAnalysisRepository.existsByAnalysis_Id(20)).thenReturn(false);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.createBatchPullRequest(
                        new BatchPullRequestRequest(List.of(20, 21), "main", null, null), 1));

        assertEquals(ErrorCode.BATCH_COMMIT_MISMATCH, exception.getErrorCode());
        verify(githubService, never()).createPullRequest(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createsPullRequestForSinglePushedAnalysis() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder()
                .id(10).organization("aivle").name("BigP-Back").build();
        Analysis analysis = pushedAnalysis(20, user, repo, "dev", "src/A.java", "commit-sha");
        GithubPullRequestResult result = pullRequestResult("commit-sha");
        stubBatchPullRequestPrerequisites(List.of(analysis));
        when(githubService.createPullRequest(
                1, "aivle", "BigP-Back", "dev", "main", "AI 개선", "선택 반영"))
                .thenReturn(result);
        when(githubPullRequestRepository.findTrackedPullRequest("aivle", "BigP-Back", 42))
                .thenReturn(Optional.empty());
        when(githubPullRequestRepository.save(any(GithubPullRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = analysisService.createBatchPullRequest(
                new BatchPullRequestRequest(
                        List.of(20), "main", "AI 개선", "선택 반영"), 1);

        assertEquals(42, response.pullRequestNumber());
        assertEquals(List.of(20), response.analysisIds());
        assertEquals(false, response.recovered());
        assertLinkedAnalysisIds(List.of(20));
    }

    @Test
    void connectsMultiplePushedAnalysesToOnePullRequest() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder()
                .id(10).organization("aivle").name("BigP-Back").build();
        Analysis first = pushedAnalysis(20, user, repo, "dev", "src/A.java", "commit-sha");
        Analysis second = pushedAnalysis(21, user, repo, "dev", "src/B.java", "commit-sha");
        GithubPullRequestResult result = pullRequestResult("commit-sha");
        stubBatchPullRequestPrerequisites(List.of(first, second));
        when(githubService.createPullRequest(
                1, "aivle", "BigP-Back", "dev", "main", "AI 개선", "선택 반영"))
                .thenReturn(result);
        when(githubPullRequestRepository.findTrackedPullRequest("aivle", "BigP-Back", 42))
                .thenReturn(Optional.empty());
        when(githubPullRequestRepository.save(any(GithubPullRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = analysisService.createBatchPullRequest(
                new BatchPullRequestRequest(
                        List.of(20, 21), "main", "AI 개선", "선택 반영"), 1);

        assertEquals(List.of(20, 21), response.analysisIds());
        assertLinkedAnalysisIds(List.of(20, 21));
    }

    @Test
    void recoversExistingPullRequestWithoutPushingAgain() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder()
                .id(10).organization("aivle").name("BigP-Back").build();
        Analysis analysis = pushedAnalysis(20, user, repo, "dev", "src/A.java", "commit-sha");
        GithubPullRequestResult result = pullRequestResult("commit-sha");
        stubBatchPullRequestPrerequisites(List.of(analysis));
        when(githubService.createPullRequest(
                1, "aivle", "BigP-Back", "dev", "main", "AI 개선", "선택 반영"))
                .thenThrow(new CustomException(ErrorCode.GITHUB_PR_ALREADY_OPEN));
        when(githubService.findOpenPullRequest(
                1, "aivle", "BigP-Back", "dev", "main"))
                .thenReturn(Optional.of(result));
        when(githubPullRequestRepository.findTrackedPullRequest("aivle", "BigP-Back", 42))
                .thenReturn(Optional.empty());
        when(githubPullRequestRepository.save(any(GithubPullRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = analysisService.createBatchPullRequest(
                new BatchPullRequestRequest(
                        List.of(20), "main", "AI 개선", "선택 반영"), 1);

        assertEquals(true, response.recovered());
        assertEquals("commit-sha", response.headCommitSha());
        verify(githubService, never()).createBlob(any(), any(), any(), any());
        verify(githubService, never()).updateBranchHead(any(), any(), any(), any(), any());
        assertLinkedAnalysisIds(List.of(20));
    }

    @Test
    void closesNewPullRequestWhenGithubCreatedItFromUnexpectedHead() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder()
                .id(10).organization("aivle").name("BigP-Back").build();
        Analysis analysis = pushedAnalysis(20, user, repo, "dev", "src/A.java", "commit-sha");
        stubBatchPullRequestPrerequisites(List.of(analysis));
        when(githubService.createPullRequest(
                1, "aivle", "BigP-Back", "dev", "main", "AI 개선", "선택 반영"))
                .thenReturn(new GithubPullRequestResult(
                        42, "https://github.com/aivle/BigP-Back/pull/42",
                        "OPEN", false, "unexpected-head"));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.createBatchPullRequest(
                        new BatchPullRequestRequest(
                                List.of(20), "main", "AI 개선", "선택 반영"), 1));

        assertEquals(ErrorCode.GITHUB_PR_HEAD_MISMATCH, exception.getErrorCode());
        verify(githubService).closePullRequest(1, "aivle", "BigP-Back", 42);
        verify(pullRequestAnalysisRepository, never()).saveAll(any());
    }

    private void stubBatchPullRequestPrerequisites(List<Analysis> analyses) {
        List<Integer> ids = analyses.stream().map(Analysis::getId).toList();
        Analysis first = analyses.get(0);
        GithubRepo repo = first.getGithubRepo();
        when(analysisRepository.findAllById(ids)).thenReturn(analyses);
        analyses.forEach(analysis ->
                when(pullRequestAnalysisRepository.existsByAnalysis_Id(analysis.getId()))
                        .thenReturn(false));
        when(githubService.getBranchHeadSha(
                first.getUser().getId(), repo.getOrganization(), repo.getName(), first.getBranch()))
                .thenReturn(first.getPushedCommitSha());
    }

    private void stubLatestFiles(List<Analysis> analyses) {
        analyses.forEach(analysis -> when(githubService.getLatestFileContent(
                analysis.getUser().getId(),
                analysis.getGithubRepo().getId(),
                analysis.getFilePath(),
                "head-sha"))
                .thenReturn(new GithubFileContent("original", analysis.getSourceBlobSha())));
        Map<String, String> modes = new HashMap<>();
        for (int index = 0; index < analyses.size(); index++) {
            modes.put(analyses.get(index).getFilePath(), index == 0 ? "100644" : "100755");
        }
        Analysis first = analyses.get(0);
        when(githubService.getFileModes(
                first.getUser().getId(),
                first.getGithubRepo().getOrganization(),
                first.getGithubRepo().getName(),
                "base-tree-sha",
                modes.keySet()))
                .thenReturn(modes);
    }

    @SuppressWarnings("unchecked")
    private void assertLinkedAnalysisIds(List<Integer> expectedIds) {
        ArgumentCaptor<Iterable<PullRequestAnalysis>> captor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(pullRequestAnalysisRepository).saveAll(captor.capture());
        List<Integer> actualIds = StreamSupport
                .stream(captor.getValue().spliterator(), false)
                .map(link -> link.getAnalysis().getId())
                .toList();
        assertEquals(expectedIds, actualIds);
    }

    private Analysis pushedAnalysis(
            Integer id,
            User user,
            GithubRepo repo,
            String branch,
            String filePath,
            String commitSha
    ) {
        Analysis analysis = completedAnalysis(id, user, repo, branch, filePath);
        analysis.markPushed(commitSha, java.time.LocalDateTime.now());
        return analysis;
    }

    private GithubPullRequestResult pullRequestResult(String headCommitSha) {
        return new GithubPullRequestResult(
                42,
                "https://github.com/aivle/BigP-Back/pull/42",
                "OPEN",
                false,
                headCommitSha);
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
                .sourceBlobSha("source-sha-" + id)
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
