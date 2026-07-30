package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.myspace.MySpaceAnalysisResponse;
import com.aivle.bigproject.dto.myspace.MySpaceSummaryResponse;
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
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MySpaceService {

    private final UserRepoRepository userRepoRepository;
    private final AnalysisRepository analysisRepository;
    private final FindingRepository findingRepository;

    public MySpaceService(
            UserRepoRepository userRepoRepository,
            AnalysisRepository analysisRepository,
            FindingRepository findingRepository
    ) {
        this.userRepoRepository = userRepoRepository;
        this.analysisRepository = analysisRepository;
        this.findingRepository = findingRepository;
    }

    public MySpaceSummaryResponse getSummary(Integer userId, Integer repoId, String branch) {
        validateBranch(branch);
        UserRepo userRepo = getConnectedRepo(userId, repoId);
        GithubRepo repo = userRepo.getGithubRepo();
        List<Analysis> analyses = analysisRepository.findRecentCompletedForMySpace(
                userId, repoId, branch, PageRequest.of(0, 2));
        if (analyses.isEmpty()) {
            return emptySummary(repo, branch);
        }

        Analysis current = analyses.get(0);
        Finding currentFinding = findingRepository.findByAnalysisId(current.getId()).orElse(null);
        Analysis previous = analyses.size() > 1 ? analyses.get(1) : null;
        Finding previousFinding = previous == null
                ? null
                : findingRepository.findByAnalysisId(previous.getId()).orElse(null);

        double currentQualityScore = qualityScore(currentFinding);
        double previousQualityScore = qualityScore(previousFinding);
        BigDecimal currentRatio = ratio(current);
        BigDecimal previousRatio = ratio(previous);

        return new MySpaceSummaryResponse(
                repo.getId(),
                repo.getName(),
                branch,
                current.getId(),
                current.getCreatedAt(),
                current.getFilePath(),
                totalIssues(currentFinding),
                securityIssues(currentFinding),
                inefficiencyIssues(currentFinding),
                otherIssues(currentFinding),
                current.getImprovableRatio(),
                currentQualityScore,
                comparison(
                        currentFinding, previous, previousFinding,
                        currentRatio, previousRatio,
                        currentQualityScore, previousQualityScore),
                currentFinding == null ? null : currentFinding.getSecuResult(),
                currentFinding == null ? null : currentFinding.getInefficiencyResult(),
                current.getOriginCode(),
                currentFinding == null ? null : currentFinding.getModifiedCode()
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

    private UserRepo getConnectedRepo(Integer userId, Integer repoId) {
        return userRepoRepository.findByUser_IdAndGithubRepo_Id(userId, repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));
    }

    private void validateBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_BRANCH);
        }
    }

    private MySpaceSummaryResponse emptySummary(GithubRepo repo, String branch) {
        return new MySpaceSummaryResponse(
                repo.getId(), repo.getName(), branch,
                null, null, null,
                0, 0, 0, 0,
                null, 0,
                new MySpaceSummaryResponse.Comparison(null, null, null, null, null),
                null, null, null, null);
    }

    private MySpaceSummaryResponse.Comparison comparison(
            Finding currentFinding,
            Analysis previous,
            Finding previousFinding,
            BigDecimal currentRatio,
            BigDecimal previousRatio,
            double currentQualityScore,
            double previousQualityScore
    ) {
        if (previous == null) {
            return new MySpaceSummaryResponse.Comparison(null, null, null, null, null);
        }
        return new MySpaceSummaryResponse.Comparison(
                totalIssues(currentFinding) - totalIssues(previousFinding),
                securityIssues(currentFinding) - securityIssues(previousFinding),
                inefficiencyIssues(currentFinding) - inefficiencyIssues(previousFinding),
                currentRatio.subtract(previousRatio),
                roundOneDecimal(currentQualityScore - previousQualityScore));
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

    private BigDecimal ratio(Analysis analysis) {
        return analysis == null || analysis.getImprovableRatio() == null
                ? BigDecimal.ZERO.setScale(1)
                : analysis.getImprovableRatio();
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
}
