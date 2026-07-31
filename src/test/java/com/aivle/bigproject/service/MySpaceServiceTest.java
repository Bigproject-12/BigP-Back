package com.aivle.bigproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Finding;
import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.UserRepo;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.FindingRepository;
import com.aivle.bigproject.repository.UserRepoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class MySpaceServiceTest {

    @Mock UserRepoRepository userRepoRepository;
    @Mock AnalysisRepository analysisRepository;
    @Mock FindingRepository findingRepository;
    @Spy JsonMapper jsonMapper = JsonMapper.builder().build();
    @InjectMocks MySpaceService mySpaceService;

    @Test
    void aggregatesLatestAnalysisForEachFileAndComparesWithPreviousSet() {
        GithubRepo repo = GithubRepo.builder().id(10).name("BigP-Back").build();
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 10))
                .thenReturn(Optional.of(UserRepo.builder().githubRepo(repo).build()));
        Analysis fileACurrent = analysis(20, "src/UserService.java", "40.0",
                LocalDateTime.of(2026, 7, 30, 10, 0));
        Analysis fileBCurrent = analysis(19, "src/UserController.java", "20.0",
                LocalDateTime.of(2026, 7, 30, 9, 0));
        Analysis fileAPrevious = analysis(18, "src/UserService.java", "30.0",
                LocalDateTime.of(2026, 7, 29, 10, 0));
        Analysis fileBPrevious = analysis(17, "src/UserController.java", "10.0",
                LocalDateTime.of(2026, 7, 29, 9, 0));
        List<Analysis> analyses = List.of(
                fileACurrent, fileBCurrent, fileAPrevious, fileBPrevious);
        when(analysisRepository.findLatestTwoPerFile(1, 10, "dev"))
                .thenReturn(analyses);
        when(findingRepository.findAllByAnalysisIdIn(List.of(20, 19, 18, 17)))
                .thenReturn(List.of(
                        finding(fileACurrent, 10, 2, 3),
                        finding(fileBCurrent, 4, 1, 1),
                        finding(fileAPrevious, 12, 3, 3),
                        finding(fileBPrevious, 6, 1, 2)));

        var response = mySpaceService.getSummary(1, 10, "dev");

        assertEquals(20, response.analysisId());
        assertEquals(14, response.totalIssueCount());
        assertEquals(3, response.securityIssueCount());
        assertEquals(4, response.inefficiencyIssueCount());
        assertEquals(7, response.otherIssueCount());
        assertEquals(new BigDecimal("30.0"), response.improvableRatio());
        assertEquals(64.5, response.qualityScore());
        assertEquals(-4, response.comparison().totalIssueChange());
        assertEquals(new BigDecimal("10.0"), response.comparison().improvableRatioChange());
        assertEquals(10.5, response.comparison().qualityScoreChange());
    }

    @Test
    void rejectsRepositoryNotConnectedToCurrentUser() {
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 99))
                .thenReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> mySpaceService.getSummary(1, 99, "dev"));

        assertEquals(ErrorCode.REPO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void returnsLatestFileAnalysisWithStructuredIssues() {
        GithubRepo repo = GithubRepo.builder().id(10).name("BigP-Back").build();
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 10))
                .thenReturn(Optional.of(UserRepo.builder().githubRepo(repo).build()));
        Analysis analysis = analysis(20, "src/UserService.java", "40.0",
                LocalDateTime.of(2026, 7, 30, 10, 0));
        analysis.setGithubRepo(repo);
        when(analysisRepository.findLatestCompletedFile(
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(10),
                org.mockito.ArgumentMatchers.eq("dev"),
                org.mockito.ArgumentMatchers.eq("src/UserService.java"),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(analysis));
        Finding finding = finding(analysis, 2, 1, 1);
        finding.setSecuResult("[{\"rule_id\":\"java.sql\",\"message\":\"SQL 입력값을 검증하세요.\",\"line\":12}]");
        finding.setInefficiencyResult("[{\"function_name\":\"login\",\"complexity_score\":18,\"line\":30,\"message\":\"조건문을 분리하세요.\"}]");
        when(findingRepository.findByAnalysisId(20)).thenReturn(Optional.of(finding));

        var response = mySpaceService.getLatestFileAnalysis(
                1, 10, "dev", "src/UserService.java");

        assertEquals(20, response.analysisId());
        assertEquals(1, response.securityIssues().size());
        assertEquals("HIGH", response.securityIssues().get(0).severity());
        assertEquals(12, response.securityIssues().get(0).line());
        assertEquals("login", response.inefficiencyIssues().get(0).functionName());
        assertEquals("HIGH", response.inefficiencyIssues().get(0).severity());
        assertEquals("class UserService {}", response.originCode());
    }

    @Test
    void rejectsAnalysisHistoryOwnedByAnotherUser() {
        when(analysisRepository.findOwnedAnalysis(20, 1)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> mySpaceService.getAnalysisDetail(1, 20));

        assertEquals(ErrorCode.ANALYSIS_NOT_FOUND, exception.getErrorCode());
    }

    private Analysis analysis(int id, String filePath, String ratio, LocalDateTime createdAt) {
        return Analysis.builder()
                .id(id)
                .branch("dev")
                .filePath(filePath)
                .status("COMPLETED")
                .originCode("class UserService {}")
                .improvableRatio(new BigDecimal(ratio))
                .createdAt(createdAt)
                .build();
    }

    private Finding finding(
            Analysis analysis,
            int total,
            int security,
            int inefficiency
    ) {
        return Finding.builder()
                .analysis(analysis)
                .totalIssues(total)
                .securityCount(security)
                .inefficiencyCount(inefficiency)
                .secuResult("[]")
                .inefficiencyResult("[]")
                .modifiedCode("class UserService { /* improved */ }")
                .build();
    }
}
