package com.aivle.bigproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MySpaceServiceTest {

    @Mock UserRepoRepository userRepoRepository;
    @Mock AnalysisRepository analysisRepository;
    @Mock FindingRepository findingRepository;
    @InjectMocks MySpaceService mySpaceService;

    @Test
    void returnsLatestPersonalAnalysisSummaryAndComparison() {
        GithubRepo repo = GithubRepo.builder().id(10).name("BigP-Back").build();
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 10))
                .thenReturn(Optional.of(UserRepo.builder().githubRepo(repo).build()));
        Analysis current = analysis(20, "40.0", LocalDateTime.of(2026, 7, 30, 10, 0));
        Analysis previous = analysis(19, "30.0", LocalDateTime.of(2026, 7, 29, 10, 0));
        when(analysisRepository.findRecentCompletedForMySpace(
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(10),
                org.mockito.ArgumentMatchers.eq("dev"),
                any(Pageable.class)))
                .thenReturn(List.of(current, previous));
        when(findingRepository.findByAnalysisId(20)).thenReturn(Optional.of(
                finding(current, 10, 2, 3)));
        when(findingRepository.findByAnalysisId(19)).thenReturn(Optional.of(
                finding(previous, 12, 3, 3)));

        var response = mySpaceService.getSummary(1, 10, "dev");

        assertEquals(20, response.analysisId());
        assertEquals(10, response.totalIssueCount());
        assertEquals(5, response.otherIssueCount());
        assertEquals(50.0, response.qualityScore());
        assertEquals(-2, response.comparison().totalIssueChange());
        assertEquals(new BigDecimal("10.0"), response.comparison().improvableRatioChange());
        assertEquals(13.0, response.comparison().qualityScoreChange());
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

    private Analysis analysis(int id, String ratio, LocalDateTime createdAt) {
        return Analysis.builder()
                .id(id)
                .branch("dev")
                .filePath("src/UserService.java")
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
