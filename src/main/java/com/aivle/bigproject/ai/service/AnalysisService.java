package com.aivle.bigproject.ai.service;

// DTO
import com.aivle.bigproject.ai.dto.DetectRequest;
import com.aivle.bigproject.ai.dto.DetectResponse;
import com.aivle.bigproject.ai.dto.DuplicateSnippet;
import com.aivle.bigproject.ai.dto.AnalysisResultResponse;
import com.aivle.bigproject.dto.repo.GithubPullRequestResult;

// Entity
import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Company;
import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.entity.Finding;
import com.aivle.bigproject.entity.GithubPullRequest;
import com.aivle.bigproject.entity.PullRequestAnalysis;

// Repository
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.CompanyRepository;
import com.aivle.bigproject.repository.GithubRepoRepository;
import com.aivle.bigproject.repository.UserRepository;
import com.aivle.bigproject.repository.FindingRepository;
import com.aivle.bigproject.repository.GithubPullRequestRepository;
import com.aivle.bigproject.repository.PullRequestAnalysisRepository;

// Exception
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;

// Service
import com.aivle.bigproject.service.NotificationService;
import com.aivle.bigproject.ai.service.EmbeddingService;
import com.aivle.bigproject.service.GithubService;

import org.springframework.beans.propertyeditors.CustomNumberEditor;
// Spring Web
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@Service
public class AnalysisService {

    // AI 서버 주소
    private final String AI_DETECT_URL = "http://localhost:8000/api/ai/detect";

    private final AnalysisRepository analysisRepository;
    private final GithubRepoRepository githubRepoRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final FindingRepository findingRepository;
    private final NotificationService notificationService;
    private final JsonMapper jsonMapper;
    private final EmbeddingService embeddingService;
    private final GithubService githubService;
    private final GithubPullRequestRepository githubPullRequestRepository;
    private final PullRequestAnalysisRepository pullRequestAnalysisRepository;

    public AnalysisService(AnalysisRepository analysisRepository, 
                           GithubRepoRepository githubRepoRepository, 
                           CompanyRepository companyRepository, 
                           UserRepository userRepository,
                           FindingRepository findingRepository,
                           NotificationService notificationService,
                           EmbeddingService embeddingService,
                           JsonMapper jsonMapper,
                           GithubService githubService,
                           GithubPullRequestRepository githubPullRequestRepository,
                           PullRequestAnalysisRepository pullRequestAnalysisRepository) {
        this.analysisRepository = analysisRepository;
        this.githubRepoRepository = githubRepoRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.findingRepository = findingRepository;
        this.notificationService = notificationService;
        this.embeddingService = embeddingService;
        this.jsonMapper = jsonMapper;
        this.githubService = githubService;
        this.githubPullRequestRepository = githubPullRequestRepository;
        this.pullRequestAnalysisRepository = pullRequestAnalysisRepository;
    }

    public Integer createInitialAnalysis(DetectRequest requestDto, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Company company = user.getCompany();

        GithubRepo repo = githubRepoRepository.findById(requestDto.repoId())
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));
                
        if (company == null) {
            throw new CustomException(ErrorCode.COMPANY_NOT_LINKED);
        }

        // 분석 중 상태로 DB에 저장
        Analysis analysis = Analysis.builder()
                .githubRepo(repo)
                .company(company)
                .user(user)
                .originCode(requestDto.codeContent())
                .language(requestDto.language())
                .filePath(requestDto.filePath()) // 히스토리관련 추가
                .branch(requestDto.branch())
                .prompt(null) 
                .status("ANALYZING") // 초기 생성 시 곧바로 ANALYZING 처리
                .build();
                
        analysisRepository.save(analysis);

        // 생성된 ID 반환
        return analysis.getId();
    }


    @Async
    public void sendToAiServerAsync(Integer analysisId, DetectRequest requestDto) {
        List<DuplicateSnippet> duplicates = requestDto.repoId() != null
                ? embeddingService.searchDuplicates(requestDto.repoId(), requestDto.codeContent())
                : List.of();


        DetectRequest enrichedRequest = new DetectRequest(
                requestDto.codeContent(),
                requestDto.repoId(),
                requestDto.language(),
                requestDto.prompt(),
                requestDto.filePath(),
                requestDto.branch(),
                duplicates
        );
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<DetectRequest> requestEntity = new HttpEntity<>(enrichedRequest, headers);

        try {
            DetectResponse response = restTemplate.postForObject(AI_DETECT_URL, requestEntity, DetectResponse.class);
            
            Analysis currentAnalysis = analysisRepository.findById(analysisId).orElseThrow();
            
            if ("CANCELED".equals(currentAnalysis.getStatus())) {
                System.out.println("사용자가 분석을 취소했으므로 결과를 저장하지 않습니다.");
                return;
            }
            
            int securityCount = response.vulnerabilities() != null ? response.vulnerabilities().size() : 0;
            int inefficiencyCount = response.complexityDetails() != null ? response.complexityDetails().size() : 0;
            
            // 조건 없이 항상 Finding 저장 (null 방어 포함)
            String secuResultStr = jsonMapper.writeValueAsString(
                    response.vulnerabilities() != null ? response.vulnerabilities() : List.of()
            );
            String inefficiencyResultStr = jsonMapper.writeValueAsString(
                    response.complexityDetails() != null ? response.complexityDetails() : List.of()
            );
            String modifiedCode = response.patchedCode() != null ? response.patchedCode() : "";

            Finding finding = Finding.builder()
                    .analysis(currentAnalysis)
                    .inefficiencyResult(inefficiencyResultStr)
                    .modifiedCode(modifiedCode)
                    .secuResult(secuResultStr)
                    .duplicateResult("[]") 
                    .isAiGenerated(response.isAiGenerated() != null && response.isAiGenerated())
                    .aiProbability(response.aiProbability())
                    .totalIssues(securityCount + inefficiencyCount)
                    .securityCount(securityCount)
                    .inefficiencyCount(inefficiencyCount)
                    .build();

            findingRepository.save(finding);
            // 개선 가능률 계산
            try {
                BigDecimal improvableRatio = calculateImprovableRatio(
                        currentAnalysis.getOriginCode(),modifiedCode );
                currentAnalysis.setImprovableRatio(improvableRatio);
            } catch (Exception e) {
                System.out.println("[improvableRatio] 계산 실패, null로 유지: " + e.getMessage());
            }

            currentAnalysis.setStatus("COMPLETED");
            analysisRepository.save(currentAnalysis);
            notificationService.notifyAnalysisCompleted(currentAnalysis);
            
        } catch (Exception e) {
            Analysis currentAnalysis = analysisRepository.findById(analysisId).orElseThrow();
            currentAnalysis.setStatus("FAILED");
            analysisRepository.save(currentAnalysis);
            notificationService.notifyAnalysisFailed(currentAnalysis);
            e.printStackTrace();
        }
    }

 
    public void stopAnalysis(Integer analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_NOT_FOUND));

        if ("COMPLETED".equals(analysis.getStatus()) || "FAILED".equals(analysis.getStatus())) {
            throw new CustomException(ErrorCode.ANALYSIS_ALREADY_FINISHED);
        }

        analysis.setStatus("CANCELED");
        analysisRepository.save(analysis);
    }

    /**
     * 분석 결과 반환
     */
    public AnalysisResultResponse getAnalysisResult(Integer analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException((ErrorCode.ANALYSIS_NOT_FOUND)));

        Finding finding = findingRepository.findByAnalysisId(analysisId).orElse(null);

        return AnalysisResultResponse.of(analysis, finding);
    }

    public void pushImprovedCode(Integer analysisId, Integer userId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_NOT_FOUND));

        if (!analysis.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
        if (!"COMPLETED".equals(analysis.getStatus())) {
            throw new CustomException(ErrorCode.ANALYSIS_NOT_COMPLETED);
        }
        if (analysis.getBranch() == null || analysis.getFilePath() == null) {
            throw new CustomException(ErrorCode.ANALYSIS_BRANCH_FILE_INFO_MISSING);
        }

        Finding finding = findingRepository.findByAnalysisId(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.FINDING_NOT_FOUND));

        GithubRepo repo = analysis.getGithubRepo();

        String sha = githubService.getFileSha(
                userId, repo.getOrganization(), repo.getName(), analysis.getFilePath(), analysis.getBranch());
        
        String codeTocommit = (finding.getModifiedCode() != null &&
        !finding.getModifiedCode().isBlank())
                ? finding.getModifiedCode() : analysis.getOriginCode();

        githubService.commitFile(
                userId, repo.getOrganization(), repo.getName(), analysis.getFilePath(), analysis.getBranch(),
                codeTocommit, sha, "GuardrAil: AI 코드 개선 반영 (분석 ID: " + analysisId + ")");
    }

    @Transactional
    public String createPullRequest(Integer analysisId, Integer userId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_NOT_FOUND));

        if (!analysis.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
        if (!"COMPLETED".equals(analysis.getStatus())) {
            throw new CustomException(ErrorCode.ANALYSIS_NOT_COMPLETED);
        }
        if (analysis.getBranch() == null) {
            throw new CustomException(ErrorCode.ANALYSIS_BRANCH_INFO_MISSING);
        }
        if (pullRequestAnalysisRepository.existsByAnalysis_Id(analysisId)) {
            throw new CustomException(ErrorCode.GITHUB_PR_ALREADY_CREATED);
        }

        Finding finding = findingRepository.findByAnalysisId(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.FINDING_NOT_FOUND));

        GithubRepo repo = analysis.getGithubRepo();
        String defaultBranch = githubService.getDefaultBranch(userId, repo.getOrganization(), repo.getName());

        if (analysis.getBranch().equals(defaultBranch)) {
            throw new CustomException(ErrorCode.GITHUB_PR_SAME_BRANCH);
        }

        String fileName = analysis.getFilePath() != null
                ? analysis.getFilePath().substring(analysis.getFilePath().lastIndexOf('/') + 1)
                : "코드";

        String title = "GuardrAil: " + fileName + " 코드 개선 (이슈 " + finding.getTotalIssues() + "건)";
        String body = "분석 결과: 총 " + finding.getTotalIssues() + "건의 이슈 개선.\n\n"
                + "- 파일: `" + analysis.getFilePath() + "`\n"
                + "- 보안 이슈: " + finding.getSecurityCount() + "건\n"
                + "- 비효율 이슈: " + finding.getInefficiencyCount() + "건\n\n"
                + "분석 세부 내용은 분석 ID: " + analysisId + "에서 확인 가능.";

        GithubPullRequestResult result = githubService.createPullRequest(
                userId, repo.getOrganization(), repo.getName(), analysis.getBranch(), defaultBranch, title, body);

        GithubPullRequest pullRequest = githubPullRequestRepository.save(
                GithubPullRequest.builder()
                        .user(analysis.getUser())
                        .githubRepo(repo)
                        .githubPrNumber(result.number())
                        .title(title)
                        .description(body)
                        .headBranch(analysis.getBranch())
                        .baseBranch(defaultBranch)
                        .status(result.status())
                        .draft(result.draft())
                        .prUrl(result.url())
                        .headCommitSha(result.headCommitSha())
                        .build()
        );
        pullRequestAnalysisRepository.save(
                PullRequestAnalysis.builder()
                        .pullRequest(pullRequest)
                        .analysis(analysis)
                        .build()
        );

        return result.url();
    }
    // 개선 가능률 계산  = (원본에서 바뀌거나 사라진 줄 수 / 원본 전체 줄 수) * 100
    private BigDecimal calculateImprovableRatio(String originCode, String modifiedCode) {

        BigDecimal zero = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);  // 0.0

        // 코드가 비면 계산 불가
        if (originCode == null || originCode.isBlank()){
            return zero;
        }

        if (modifiedCode == null || modifiedCode.isBlank()) {
            return null;
        }

        // 분모 : 원본 전체 줄 수
        int totalLines = originCode.split("\r?\n", -1).length;
        if (totalLines == 0) {
            return zero;
        }

        // 분자는 유효 줄만 사용 -> 빈 줄 매칭 노이즈 방지
        List<String> originLines = toEffectiveLines(originCode);

        Map<String, Integer> modifiedCounts = new HashMap<>();
        for (String line : toEffectiveLines(modifiedCode)) {
            modifiedCounts.merge(line, 1, Integer::sum);
        }

        int changedLines = 0;
        for (String line : originLines) {
            Integer remaining = modifiedCounts.get(line);
            if (remaining != null && remaining > 0) {
                modifiedCounts.put(line, remaining - 1);
            } else {
                changedLines++;
            }
        }

        if (changedLines == 0) {
            return zero;
        }

        BigDecimal ratio = BigDecimal.valueOf(changedLines)
                .divide(BigDecimal.valueOf(totalLines), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);

        return ratio.min(BigDecimal.valueOf(100).setScale(1, RoundingMode.HALF_UP));
    }

    /**
     * 비교 대상이 되는 유효 줄만 추린다.
     * 빈 줄과 중괄호만 있는 줄은 어느 코드에나 흔해서 비교를 왜곡시킨다.
     */
    private List<String> toEffectiveLines(String code) {
        List<String> lines = new ArrayList<>();
        for (String raw : code.split("\r?\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.equals("}") || line.equals("{")) continue;
            lines.add(line);
        }
        return lines;
    }
}
