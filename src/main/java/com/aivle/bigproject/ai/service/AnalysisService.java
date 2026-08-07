package com.aivle.bigproject.ai.service;

// DTO
import com.aivle.bigproject.ai.dto.DetectRequest;
import com.aivle.bigproject.ai.dto.DetectResponse;
import com.aivle.bigproject.ai.dto.DuplicateSnippet;
import com.aivle.bigproject.ai.dto.PromptReconstructApiRequest;
import com.aivle.bigproject.ai.dto.PromptReconstructApiResponse;
import com.aivle.bigproject.ai.dto.AnalysisResultResponse;
import com.aivle.bigproject.ai.dto.ReanalysisStart;
import com.aivle.bigproject.dto.repo.GithubPullRequestResult;
import com.aivle.bigproject.dto.repo.GithubFileContent;
import com.aivle.bigproject.dto.analysis.BatchPushResponse;
import com.aivle.bigproject.dto.analysis.BatchPushRequest;
import com.aivle.bigproject.dto.analysis.BatchPullRequestRequest;
import com.aivle.bigproject.dto.analysis.BatchPullRequestResponse;
import com.aivle.bigproject.dto.repo.GithubTreeItem;

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
import tools.jackson.core.type.TypeReference;
import com.aivle.bigproject.service.GithubService;
import com.aivle.bigproject.ai.service.EmbeddingService;

// Spring Web
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Objects;


@Service
public class AnalysisService {

    @Value("${ai.base-url}")
    private String aiBaseUrl;

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
        System.out.println("[TRACE] createInitialAnalysis 진입, requestDto.repoId()=" + requestDto.repoId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Company company = user.getCompany();

        GithubRepo repo = githubRepoRepository.findById(requestDto.repoId())
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));
                
        if (company == null) {
            throw new CustomException(ErrorCode.COMPANY_NOT_LINKED);
        }

        GithubFileContent sourceFile = githubService.getLatestFileContent(
                userId, repo.getId(), requestDto.filePath(), requestDto.branch());

        // 분석 중 상태로 DB에 저장
        Analysis analysis = Analysis.builder()
                .githubRepo(repo)
                .company(company)
                .user(user)
                .originCode(requestDto.codeContent())
                .language(requestDto.language())
                .filePath(requestDto.filePath()) // 히스토리관련 추가
                .branch(requestDto.branch())
                .sourceBlobSha(sourceFile.sha())
                .prompt(null) 
                .status("ANALYZING") // 초기 생성 시 곧바로 ANALYZING 처리
                .build();
                
        analysisRepository.save(analysis);

        // 생성된 ID 반환
        return analysis.getId();
    }


    @Async
    public void sendToAiServerAsync(Integer analysisId, DetectRequest requestDto) {
        System.out.println("[TRACE][" + Thread.currentThread().getName() + "] sendToAiServerAsync 진입, repoId=" + requestDto.repoId());
        try {
            System.out.println("[TRACE] 2. try 블록 진입");
            System.out.println("[TRACE] 2-1. repoId null 체크 직전: " + requestDto.repoId());

            List<DuplicateSnippet> duplicates;
            if (requestDto.repoId() != null) {
                System.out.println("[TRACE] 2-2. if 블록 진입 (repoId not null)");
                duplicates = embeddingService.searchDuplicates(requestDto.repoId(), requestDto.codeContent(), requestDto.language());
                System.out.println("[TRACE] 2-3. searchDuplicates 리턴됨");
            } else {
                System.out.println("[TRACE] 2-2-B. else 블록 진입 (repoId is null)");
                duplicates = List.of();
            }
            System.out.println("[TRACE] duplicates 검색 결과 개수: " + duplicates.size());
            DetectRequest enrichedRequest = new DetectRequest(
                    requestDto.codeContent(),
                    requestDto.repoId(),
                    requestDto.language(),
                    requestDto.prompt(),
                    requestDto.filePath(),
                    requestDto.branch(),
                    duplicates
            );
            // ponytail: AI 서버가 응답 없이 멈추면 무한 대기 → 상태가 영원히 ANALYZING으로 남음.
            // 타임아웃을 걸어 실패 시 아래 catch가 FAILED로 정리하도록 함.
            // searchDuplicates 타임아웃과 합친 총합이 프론트 폴링 타임아웃(120초)보다 "일부러" 길게 잡음:
            // 그래야 진짜 멈추는 경우 프론트가 항상 먼저 포기해서 "오래 걸립니다" 메시지를 보여주고,
            // 그 직후 DB도 FAILED로 정리됨. AI 서버 응답 시간이 늘어나면 값 조정.
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5_000);
            factory.setReadTimeout(115_000);
            RestTemplate restTemplate = new RestTemplate(factory);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<DetectRequest> requestEntity = new HttpEntity<>(enrichedRequest, headers);

            DetectResponse response = restTemplate.postForObject(
                    aiBaseUrl + "/api/ai/detect", requestEntity, DetectResponse.class);
            
            Analysis currentAnalysis = analysisRepository.findById(analysisId).orElseThrow();
            
            if ("CANCELED".equals(currentAnalysis.getStatus())) {
                System.out.println("사용자가 분석을 취소했으므로 결과를 저장하지 않습니다.");
                return;
            }

            if (response == null || !Boolean.TRUE.equals(response.patchSuccess())) {
                throw new CustomException(ErrorCode.AI_PATCH_FAILED);
            }
            
            int securityCount = response.vulnerabilities() != null ? response.vulnerabilities().size() : 0;
            int inefficiencyCount = response.complexityDetails() != null ? response.complexityDetails().size() : 0;
            int duplicateCount = response.duplicateSnippets() != null ? response.duplicateSnippets().size() : 0;
            
            String duplicateResultStr = jsonMapper.writeValueAsString(
                response.duplicateSnippets() != null ? response.duplicateSnippets() : List.of()
            );

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
                    .duplicateResult(duplicateResultStr) 
                    .isAiGenerated(response.isAiGenerated() != null && response.isAiGenerated())
                    .aiProbability(response.aiProbability())
                    .totalIssues(securityCount + inefficiencyCount + duplicateCount)
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

 
    public void stopAnalysis(Integer analysisId, Integer userId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_NOT_FOUND));

        validateOwner(analysis, userId);

        if ("COMPLETED".equals(analysis.getStatus()) || "FAILED".equals(analysis.getStatus())) {
            throw new CustomException(ErrorCode.ANALYSIS_ALREADY_FINISHED);
        }

        analysis.setStatus("CANCELED");
        analysisRepository.save(analysis);
    }

    /**
     * 분석 결과 반환
     */
    public AnalysisResultResponse getAnalysisResult(Integer analysisId, Integer userId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException((ErrorCode.ANALYSIS_NOT_FOUND)));

        validateOwner(analysis, userId);

        Finding finding = findingRepository.findByAnalysisId(analysisId).orElse(null);

        return AnalysisResultResponse.of(analysis, finding);
    }

    @Transactional
    public ReanalysisStart prepareReanalysis(Integer analysisId, Integer userId) {
        Analysis previous = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_NOT_FOUND));
        validateOwner(previous, userId);
        if (!"COMPLETED".equals(previous.getStatus())) {
            throw new CustomException(ErrorCode.ANALYSIS_NOT_COMPLETED);
        }
        if (previous.getBranch() == null || previous.getBranch().isBlank()
                || previous.getFilePath() == null || previous.getFilePath().isBlank()) {
            throw new CustomException(ErrorCode.ANALYSIS_BRANCH_FILE_INFO_MISSING);
        }
        Integer repoId = previous.getGithubRepo().getId();
        if (analysisRepository.existsAnalyzingFile(
                userId, repoId, previous.getBranch(), previous.getFilePath())) {
            throw new CustomException(ErrorCode.ANALYSIS_ALREADY_RUNNING);
        }

        GithubFileContent latestFile = githubService.getLatestFileContent(
                userId, repoId, previous.getFilePath(), previous.getBranch());
        boolean sameSha = previous.getSourceBlobSha() != null
                && previous.getSourceBlobSha().equals(latestFile.sha());
        boolean sameContent = previous.getOriginCode().equals(latestFile.content());
        if (sameSha || sameContent) {
            throw new CustomException(ErrorCode.SOURCE_NOT_CHANGED);
        }
        Analysis reanalysis = Analysis.builder()
                .githubRepo(previous.getGithubRepo())
                .company(previous.getCompany())
                .user(previous.getUser())
                .originCode(latestFile.content())
                .language(previous.getLanguage())
                .filePath(previous.getFilePath())
                .branch(previous.getBranch())
                .sourceBlobSha(latestFile.sha())
                .prompt(previous.getPrompt())
                .status("ANALYZING")
                .build();
        analysisRepository.save(reanalysis);

        DetectRequest request = new DetectRequest(
                latestFile.content(),
                repoId,
                previous.getLanguage(),
                previous.getPrompt(),
                previous.getFilePath(),
                previous.getBranch(),
                List.of());
        return new ReanalysisStart(reanalysis.getId(), request);
    }

    private void validateOwner(Analysis analysis, Integer userId) {
        if (!analysis.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
    }

    @Transactional
    public void pushImprovedCode(Integer analysisId, Integer userId) {
        pushImprovedCode(analysisId, userId, false);
    }

    @Transactional
    public void pushImprovedCode(
            Integer analysisId, Integer userId, boolean overwriteChangedFiles) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_NOT_FOUND));
        validateOwner(analysis, userId);
        if (!"COMPLETED".equals(analysis.getStatus())) {
            throw new CustomException(ErrorCode.ANALYSIS_NOT_COMPLETED);
        }
        if (StringUtils.hasText(analysis.getPushedCommitSha())) {
            throw new CustomException(ErrorCode.ANALYSIS_ALREADY_PUSHED);
        }
        if (!StringUtils.hasText(analysis.getBranch())
                || !StringUtils.hasText(analysis.getFilePath())) {
            throw new CustomException(ErrorCode.ANALYSIS_BRANCH_FILE_INFO_MISSING);
        }

        Finding finding = findingRepository.findByAnalysisId(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.FINDING_NOT_FOUND));
        String pushCode = StringUtils.hasText(finding.getModifiedCode())
                ? finding.getModifiedCode()
                : analysis.getOriginCode();
        if (pushCode == null) {
            throw new CustomException(ErrorCode.IMPROVED_CODE_MISSING);
        }

        GithubRepo repo = analysis.getGithubRepo();
        GithubFileContent latestFile = githubService.getLatestFileContent(
                userId, repo.getId(), analysis.getFilePath(), analysis.getBranch());
        boolean sourceUnchanged = Objects.equals(analysis.getSourceBlobSha(), latestFile.sha())
                || Objects.equals(analysis.getOriginCode(), latestFile.content());
        if (!sourceUnchanged && !overwriteChangedFiles) {
            throw new CustomException(ErrorCode.SOURCE_CHANGED_SINCE_ANALYSIS);
        }

        String commitSha = githubService.commitFile(
                userId,
                repo.getOrganization(),
                repo.getName(),
                analysis.getFilePath(),
                analysis.getBranch(),
                pushCode,
                latestFile.sha(),
                "GuardrAil: AI 코드 개선 반영 (분석 ID: " + analysisId + ")");
        analysis.markPushed(commitSha, LocalDateTime.now());
        analysisRepository.save(analysis);
    }

    @Transactional
    public BatchPushResponse batchPush(BatchPushRequest request, Integer userId) {
        List<Integer> ids = request.analysisIds();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new CustomException(ErrorCode.BATCH_ANALYSIS_DUPLICATED);
        }

        Map<Integer, Analysis> found = analysisRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Analysis::getId, Function.identity()));
        if (found.size() != ids.size()) {
            throw new CustomException(ErrorCode.ANALYSIS_NOT_FOUND);
        }

        List<Analysis> analyses = ids.stream().map(found::get).toList();
        Analysis first = analyses.get(0);
        Integer repoId = first.getGithubRepo().getId();
        String branch = first.getBranch();
        Set<String> filePaths = new HashSet<>();
        Map<Integer, String> pushCodes = new HashMap<>();

        for (Analysis analysis : analyses) {
            validateOwner(analysis, userId);
            if (!"COMPLETED".equals(analysis.getStatus())) {
                throw new CustomException(ErrorCode.ANALYSIS_NOT_COMPLETED);
            }
            if (StringUtils.hasText(analysis.getPushedCommitSha())) {
                throw new CustomException(ErrorCode.ANALYSIS_ALREADY_PUSHED);
            }
            if (!StringUtils.hasText(analysis.getBranch())
                    || !StringUtils.hasText(analysis.getFilePath())) {
                throw new CustomException(ErrorCode.ANALYSIS_BRANCH_FILE_INFO_MISSING);
            }
            if (!repoId.equals(analysis.getGithubRepo().getId())) {
                throw new CustomException(ErrorCode.BATCH_REPOSITORY_MISMATCH);
            }
            if (!branch.equals(analysis.getBranch())) {
                throw new CustomException(ErrorCode.BATCH_BRANCH_MISMATCH);
            }
            if (!filePaths.add(analysis.getFilePath())) {
                throw new CustomException(ErrorCode.BATCH_FILE_DUPLICATED);
            }

            Finding finding = findingRepository.findByAnalysisId(analysis.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.FINDING_NOT_FOUND));
            String pushCode = StringUtils.hasText(finding.getModifiedCode())
                    ? finding.getModifiedCode()
                    : analysis.getOriginCode();
            if (pushCode == null) {
                throw new CustomException(ErrorCode.IMPROVED_CODE_MISSING);
            }
            pushCodes.put(analysis.getId(), pushCode);
        }

        GithubRepo repo = first.getGithubRepo();
        String headSha = githubService.getBranchHeadSha(
                userId, repo.getOrganization(), repo.getName(), branch);
        boolean allSourcesUnchanged = true;
        boolean allImprovedCodesApplied = true;
        for (Analysis analysis : analyses) {
            GithubFileContent latestFile = githubService.getLatestFileContent(
                    userId,
                    repoId,
                    analysis.getFilePath(),
                    headSha);
            boolean unchanged = Objects.equals(analysis.getSourceBlobSha(), latestFile.sha())
                    || Objects.equals(analysis.getOriginCode(), latestFile.content());
            allSourcesUnchanged &= unchanged;
            allImprovedCodesApplied &= Objects.equals(
                    pushCodes.get(analysis.getId()), latestFile.content());
        }

        if (!allSourcesUnchanged) {
            if (allImprovedCodesApplied) {
                LocalDateTime recoveredAt = LocalDateTime.now();
                analyses.forEach(analysis -> analysis.markPushed(headSha, recoveredAt));
                analysisRepository.saveAll(analyses);
                return new BatchPushResponse(
                        ids,
                        repoId,
                        repo.getOrganization() + "/" + repo.getName(),
                        branch,
                        headSha,
                        null,
                        null,
                        headSha,
                        recoveredAt,
                        List.of(),
                        "RECOVERED");
            }
            if (!request.overwriteChangedFiles()) {
                throw new CustomException(ErrorCode.SOURCE_CHANGED_SINCE_ANALYSIS);
            }
        }

        String baseTreeSha = githubService.getCommitTreeSha(
                userId, repo.getOrganization(), repo.getName(), headSha);
        Map<String, String> fileModes = githubService.getFileModes(
                userId,
                repo.getOrganization(),
                repo.getName(),
                baseTreeSha,
                filePaths);

        List<BatchPushResponse.FileBlob> files = new ArrayList<>();
        List<GithubTreeItem> treeItems = new ArrayList<>();
        for (Analysis analysis : analyses) {
            String blobSha = githubService.createBlob(
                    userId,
                    repo.getOrganization(),
                    repo.getName(),
                    pushCodes.get(analysis.getId()));
            files.add(new BatchPushResponse.FileBlob(
                    analysis.getId(), analysis.getFilePath(), blobSha));
            treeItems.add(new GithubTreeItem(
                    analysis.getFilePath(), blobSha, fileModes.get(analysis.getFilePath())));
        }
        String preparedTreeSha = githubService.createTree(
                userId, repo.getOrganization(), repo.getName(), baseTreeSha, treeItems);
        String commitMessage = "GuardrAil: AI 코드 개선 반영 (" + analyses.size() + "개 파일)";
        String commitSha = githubService.createCommit(
                userId,
                repo.getOrganization(),
                repo.getName(),
                commitMessage,
                preparedTreeSha,
                headSha);
        githubService.updateBranchHead(
                userId, repo.getOrganization(), repo.getName(), branch, commitSha);

        LocalDateTime pushedAt = LocalDateTime.now();
        analyses.forEach(analysis -> analysis.markPushed(commitSha, pushedAt));
        analysisRepository.saveAll(analyses);

        return new BatchPushResponse(
                ids,
                repoId,
                repo.getOrganization() + "/" + repo.getName(),
                branch,
                headSha,
                baseTreeSha,
                preparedTreeSha,
                commitSha,
                pushedAt,
                files,
                allSourcesUnchanged ? "PUSHED" : "OVERWRITE_PUSHED");
    }

    @Transactional
    public BatchPullRequestResponse createBatchPullRequest(
            BatchPullRequestRequest request, Integer userId) {
        List<Integer> ids = request.analysisIds();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new CustomException(ErrorCode.BATCH_ANALYSIS_DUPLICATED);
        }

        Map<Integer, Analysis> found = analysisRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Analysis::getId, Function.identity()));
        if (found.size() != ids.size()) {
            throw new CustomException(ErrorCode.ANALYSIS_NOT_FOUND);
        }

        List<Analysis> analyses = ids.stream().map(found::get).toList();
        Analysis first = analyses.get(0);
        GithubRepo repo = first.getGithubRepo();
        Integer repoId = repo.getId();
        String headBranch = first.getBranch();
        String pushedCommitSha = first.getPushedCommitSha();

        for (Analysis analysis : analyses) {
            validateOwner(analysis, userId);
            if (!"COMPLETED".equals(analysis.getStatus())) {
                throw new CustomException(ErrorCode.ANALYSIS_NOT_COMPLETED);
            }
            if (!StringUtils.hasText(analysis.getBranch())) {
                throw new CustomException(ErrorCode.ANALYSIS_BRANCH_INFO_MISSING);
            }
            if (!repoId.equals(analysis.getGithubRepo().getId())) {
                throw new CustomException(ErrorCode.BATCH_REPOSITORY_MISMATCH);
            }
            if (!headBranch.equals(analysis.getBranch())) {
                throw new CustomException(ErrorCode.BATCH_BRANCH_MISMATCH);
            }
            if (!StringUtils.hasText(analysis.getPushedCommitSha())) {
                throw new CustomException(ErrorCode.ANALYSIS_NOT_PUSHED);
            }
            if (!pushedCommitSha.equals(analysis.getPushedCommitSha())) {
                throw new CustomException(ErrorCode.BATCH_COMMIT_MISMATCH);
            }
            if (pullRequestAnalysisRepository.existsByAnalysis_Id(analysis.getId())) {
                throw new CustomException(ErrorCode.GITHUB_PR_ALREADY_CREATED);
            }
        }

        String baseBranch = StringUtils.hasText(request.baseBranch())
                ? request.baseBranch().trim()
                : githubService.getDefaultBranch(
                        userId, repo.getOrganization(), repo.getName());
        if (headBranch.equals(baseBranch)) {
            throw new CustomException(ErrorCode.GITHUB_PR_SAME_BRANCH);
        }

        String currentHeadSha = githubService.getBranchHeadSha(
                userId, repo.getOrganization(), repo.getName(), headBranch);
        if (!pushedCommitSha.equals(currentHeadSha)) {
            throw new CustomException(ErrorCode.GITHUB_PR_HEAD_MISMATCH);
        }

        String title = StringUtils.hasText(request.title())
                ? request.title().trim()
                : "GuardrAil: AI 코드 개선 (" + analyses.size() + "개 파일)";
        String body = StringUtils.hasText(request.body())
                ? request.body().trim()
                : buildBatchPullRequestBody(analyses);

        boolean recovered = false;
        GithubPullRequestResult result;
        try {
            result = githubService.createPullRequest(
                    userId,
                    repo.getOrganization(),
                    repo.getName(),
                    headBranch,
                    baseBranch,
                    title,
                    body);
        } catch (CustomException e) {
            if (e.getErrorCode() != ErrorCode.GITHUB_PR_ALREADY_OPEN) {
                throw e;
            }
            result = githubService.findOpenPullRequest(
                            userId,
                            repo.getOrganization(),
                            repo.getName(),
                            headBranch,
                            baseBranch)
                    .filter(pr -> pushedCommitSha.equals(pr.headCommitSha()))
                    .orElseThrow(() -> e);
            recovered = true;
        }

        if (!pushedCommitSha.equals(result.headCommitSha())) {
            if (!recovered) {
                githubService.closePullRequest(
                        userId,
                        repo.getOrganization(),
                        repo.getName(),
                        result.number());
            }
            throw new CustomException(ErrorCode.GITHUB_PR_HEAD_MISMATCH);
        }

        GithubPullRequestResult finalResult = result;
        GithubPullRequest pullRequest = githubPullRequestRepository
                .findTrackedPullRequest(
                        repo.getOrganization(), repo.getName(), finalResult.number())
                .orElseGet(() -> githubPullRequestRepository.save(
                        GithubPullRequest.builder()
                                .user(first.getUser())
                                .githubRepo(repo)
                                .githubPrNumber(finalResult.number())
                                .title(title)
                                .description(body)
                                .headBranch(headBranch)
                                .baseBranch(baseBranch)
                                .status(finalResult.status())
                                .draft(finalResult.draft())
                                .prUrl(finalResult.url())
                                .headCommitSha(finalResult.headCommitSha())
                                .build()));

        pullRequestAnalysisRepository.saveAll(analyses.stream()
                .map(analysis -> PullRequestAnalysis.builder()
                        .pullRequest(pullRequest)
                        .analysis(analysis)
                        .build())
                .toList());

        return new BatchPullRequestResponse(
                result.number(),
                result.url(),
                headBranch,
                baseBranch,
                result.headCommitSha(),
                ids,
                recovered,
                "PR_CREATED");
    }

    private String buildBatchPullRequestBody(List<Analysis> analyses) {
        return "선택한 AI 개선 코드 " + analyses.size() + "개를 반영합니다.\n\n"
                + analyses.stream()
                        .map(analysis -> "- `" + analysis.getFilePath() + "`")
                        .collect(Collectors.joining("\n"));
    }

    @Transactional
    public String createPullRequest(Integer analysisId, Integer userId, String baseBranch,
                                    String requestTitle, String requestBody) {  // 파라미터 2개 추가
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
        String targetBranch = (baseBranch != null && !baseBranch.isBlank())
                ? baseBranch
                : githubService.getDefaultBranch(userId, repo.getOrganization(), repo.getName());

        if (analysis.getBranch().equals(targetBranch)) {
            throw new CustomException(ErrorCode.GITHUB_PR_SAME_BRANCH);
        }

        // 사용자가 모달에서 값을 보냈으면 그것을, 비어 있으면 기본값을 사용
        // hasText(): null / "" / "   " 를 한 번에 걸러준다
        String title = StringUtils.hasText(requestTitle)
                ? requestTitle.trim()
                : buildDefaultTitle(analysis, finding);
        String body = StringUtils.hasText(requestBody)
                ? requestBody.trim()
                : buildDefaultBody(analysis, finding, analysisId);

        GithubPullRequestResult result = githubService.createPullRequest(
                userId, repo.getOrganization(), repo.getName(), analysis.getBranch(), targetBranch, title, body);

        GithubPullRequest pullRequest = githubPullRequestRepository.save(
                GithubPullRequest.builder()
                        .user(analysis.getUser())
                        .githubRepo(repo)
                        .githubPrNumber(result.number())
                        .title(title)          // 최종 결정된 값 그대로 저장
                        .description(body)     // 최종 결정된 값 그대로 저장
                        .headBranch(analysis.getBranch())
                        .baseBranch(targetBranch)
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

    /**
     * 사용자가 제목을 지정하지 않았을 때 쓰는 기본 PR 제목.
     */
    private String buildDefaultTitle(Analysis analysis, Finding finding) {
        String fileName = analysis.getFilePath() != null
                ? analysis.getFilePath().substring(analysis.getFilePath().lastIndexOf('/') + 1)
                : "코드";
        return "GuardrAil: " + fileName + " 코드 개선 (이슈 " + finding.getTotalIssues() + "건)";
    }
    /**
     * 사용자가 설명을 지정하지 않았을 때 쓰는 기본 PR 본문.
     */
    private String buildDefaultBody(Analysis analysis, Finding finding, Integer analysisId) {
        return "분석 결과: 총 " + finding.getTotalIssues() + "건의 이슈 개선.\n\n"
                + "- 파일: `" + analysis.getFilePath() + "`\n"
                + "- 보안 이슈: " + finding.getSecurityCount() + "건\n"
                + "- 비효율 이슈: " + finding.getInefficiencyCount() + "건\n\n"
                + "분석 세부 내용은 분석 ID: " + analysisId + "에서 확인 가능.";
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

    public PromptReconstructApiResponse reconstructPrompt(Integer analysisId, String originalPrompt) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다."));

        Finding finding = findingRepository.findByAnalysisId(analysisId)
                .orElseThrow(() -> new IllegalArgumentException("이 분석에 대한 발견 결과가 없습니다."));
        
        analysis.setPrompt(originalPrompt);
        analysisRepository.save(analysis);
        
        try {
            List<Map<String, Object>> vulnerabilities = jsonMapper.readValue(
                    finding.getSecuResult(), new TypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> complexityDetails = jsonMapper.readValue(
                    finding.getInefficiencyResult(), new TypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> duplicateSnippets = jsonMapper.readValue(
                finding.getDuplicateResult(), new TypeReference<List<Map<String, Object>>>() {});
                
            PromptReconstructApiRequest requestDto = new PromptReconstructApiRequest(
                    originalPrompt,
                    analysis.getOriginCode(),
                    vulnerabilities,
                    complexityDetails,
                    duplicateSnippets   
            );

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PromptReconstructApiRequest> entity = new HttpEntity<>(requestDto, headers);

            return restTemplate.postForObject(
                    aiBaseUrl + "/api/ai/reconstruct-prompt",
                    entity,
                    PromptReconstructApiResponse.class
            );
        } catch (Exception e) {
            throw new RuntimeException("프롬프트 재구성에 실패했습니다: " + e.getMessage());
        }
    }
}
