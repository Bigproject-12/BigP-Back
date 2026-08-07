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
import org.hibernate.engine.jdbc.batch.spi.Batch;
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
            // AI 서버가 응답 없이 멈추면 무한 대기 → 상태가 영원히 ANALYZING으로 남음.
            // 타임아웃을 걸어 실패 시 아래 catch가 FAILED로 출력
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

    // 기본 분석 결과를 기반으로 재분석 준비 진행 
    @Transactional
    public ReanalysisStart prepareReanalysis(Integer analysisId, Integer userId) {
        // 기존 분석 정보 조회 및 사용자 권한 확인
        Analysis previous = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_NOT_FOUND));
        validateOwner(previous, userId);
        //완료된 분석만 재분석이 가능하도록 설정
        if (!"COMPLETED".equals(previous.getStatus())) {
            throw new CustomException(ErrorCode.ANALYSIS_NOT_COMPLETED);
        }
        // 브랜치 및 파일 경로 확인 
        if (previous.getBranch() == null || previous.getBranch().isBlank()
                || previous.getFilePath() == null || previous.getFilePath().isBlank()) {
            throw new CustomException(ErrorCode.ANALYSIS_BRANCH_FILE_INFO_MISSING);
        }
        Integer repoId = previous.getGithubRepo().getId();
        // 동일 파일이 이미 분석 중인지 확인하고, SHA 또는 내용을 비교 후 동일하면 재분석 불가
        if (analysisRepository.existsAnalyzingFile(
                userId, repoId, previous.getBranch(), previous.getFilePath())) {
            throw new CustomException(ErrorCode.ANALYSIS_ALREADY_RUNNING);
        }

        //// GitHub에서 최신 파일 정보 조회
        GithubFileContent latestFile = githubService.getLatestFileContent(
                userId, repoId, previous.getFilePath(), previous.getBranch());
        boolean sameSha = previous.getSourceBlobSha() != null
                && previous.getSourceBlobSha().equals(latestFile.sha());
        boolean sameContent = previous.getOriginCode().equals(latestFile.content());
        // 기존 분석 이후 코드가 변경되지 않을 경우 재분석 중단 
        if (sameSha || sameContent) {
            throw new CustomException(ErrorCode.SOURCE_NOT_CHANGED);
        }
        // 새로운 재분석 정보 생성
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

        // AI 서버에 전달할 분석 요청 생성
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

    // 분석 결과의 소유자가 현재 사용자인지 확인 
    private void validateOwner(Analysis analysis, Integer userId) {
        if (!analysis.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
    }

    // AI를 통해 개선된 코드를 GitHub에 push
    // 기본적으로 원본 파일이 변경될 시 자동으로 Push되지 않도록 설정 
    @Transactional
    public void pushImprovedCode(Integer analysisId, Integer userId) {
        pushImprovedCode(analysisId, userId, false);
    }

    // 개선된 코드를 GitHub에 push, overwriteChangedFiles가 true이면 원본 파일이 변경되었더라도 강제로 push
    @Transactional
    public void pushImprovedCode(
            Integer analysisId, Integer userId, boolean overwriteChangedFiles) {
        //분석 정보
                Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_NOT_FOUND));
        // 사용자 권환 확인 
        validateOwner(analysis, userId);
        // 완료된 분석만 push 가능하도록 설정 
        if (!"COMPLETED".equals(analysis.getStatus())) {
            throw new CustomException(ErrorCode.ANALYSIS_NOT_COMPLETED);
        }
        // 이미 push된 분석인지 확인 이미 push가 된경우 중복 push 불가
        if (StringUtils.hasText(analysis.getPushedCommitSha())) {
            throw new CustomException(ErrorCode.ANALYSIS_ALREADY_PUSHED);
        }
        // 브랜치 및 파일 경로 확인
        if (!StringUtils.hasText(analysis.getBranch())
                || !StringUtils.hasText(analysis.getFilePath())) {
            throw new CustomException(ErrorCode.ANALYSIS_BRANCH_FILE_INFO_MISSING);
        }

        //// 분석 결과 조회
        Finding finding = findingRepository.findByAnalysisId(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.FINDING_NOT_FOUND));
        //// 개선 코드가 없으면 원본 코드 사용
                String pushCode = StringUtils.hasText(finding.getModifiedCode())
                ? finding.getModifiedCode()
                : analysis.getOriginCode();
        if (pushCode == null) {
            throw new CustomException(ErrorCode.IMPROVED_CODE_MISSING);
        }

        // GitHub의 최신 파일 상태 조회
        GithubRepo repo = analysis.getGithubRepo();
        GithubFileContent latestFile = githubService.getLatestFileContent(
                userId, repo.getId(), analysis.getFilePath(), analysis.getBranch());
        //// 분석 이후 원본 코드 변경 여부 확인
                boolean sourceUnchanged = Objects.equals(analysis.getSourceBlobSha(), latestFile.sha())
                || Objects.equals(analysis.getOriginCode(), latestFile.content());
        if (!sourceUnchanged && !overwriteChangedFiles) {
            throw new CustomException(ErrorCode.SOURCE_CHANGED_SINCE_ANALYSIS);
        }

        // 개선된 코드를 GitHub에 Commit
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

    // 여러 분석 결과의 개선코드를 하나의 commit으로 일광로 push 되도록 
    @Transactional
    public BatchPushResponse batchPush(BatchPushRequest request, Integer userId) {
        List<Integer> ids = request.analysisIds();
        // 중복된 분석 ID 확인
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new CustomException(ErrorCode.BATCH_ANALYSIS_DUPLICATED);
        }

        // 요청된 분석 정보 조회
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

        // Batch Push 대상 분석 검증
        for (Analysis analysis : analyses) {
            // 사용자 권한 확인
            validateOwner(analysis, userId);
            // 완료된 분석만 Push 가능
            if (!"COMPLETED".equals(analysis.getStatus())) {
                throw new CustomException(ErrorCode.ANALYSIS_NOT_COMPLETED);
            }
            // 이미 Push된 분석인지 확인
            if (StringUtils.hasText(analysis.getPushedCommitSha())) {
                throw new CustomException(ErrorCode.ANALYSIS_ALREADY_PUSHED);
            }
            // 브랜치 및 파일 정보 확인
            if (!StringUtils.hasText(analysis.getBranch())
                    || !StringUtils.hasText(analysis.getFilePath())) {
                throw new CustomException(ErrorCode.ANALYSIS_BRANCH_FILE_INFO_MISSING);
            }
            // 동일한 Repository, Branch, File Path 확인
            if (!repoId.equals(analysis.getGithubRepo().getId())) {
                throw new CustomException(ErrorCode.BATCH_REPOSITORY_MISMATCH);
            }
            if (!branch.equals(analysis.getBranch())) {
                throw new CustomException(ErrorCode.BATCH_BRANCH_MISMATCH);
            }
            if (!filePaths.add(analysis.getFilePath())) {
                throw new CustomException(ErrorCode.BATCH_FILE_DUPLICATED);
            }

            // 분석 결과 조회
            Finding finding = findingRepository.findByAnalysisId(analysis.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.FINDING_NOT_FOUND));
            // 개선 코드가 없으면 원본 코드 사용
            String pushCode = StringUtils.hasText(finding.getModifiedCode())
                    ? finding.getModifiedCode()
                    : analysis.getOriginCode();
            if (pushCode == null) {
                throw new CustomException(ErrorCode.IMPROVED_CODE_MISSING);
            }
            pushCodes.put(analysis.getId(), pushCode);
        }

        GithubRepo repo = first.getGithubRepo();
        // 현재 브랜치의 최신 Commit SHA 조회
        String headSha = githubService.getBranchHeadSha(
                userId, repo.getOrganization(), repo.getName(), branch);
        boolean allSourcesUnchanged = true;
        boolean allImprovedCodesApplied = true;
        // 분석 이후 파일 변경 여부 확인
        for (Analysis analysis : analyses) {
            GithubFileContent latestFile = githubService.getLatestFileContent(
                    userId,
                    repoId,
                    analysis.getFilePath(),
                    headSha);
            boolean unchanged = Objects.equals(analysis.getSourceBlobSha(), latestFile.sha())
                    || Objects.equals(analysis.getOriginCode(), latestFile.content());
            allSourcesUnchanged &= unchanged;
            // 개선 코드가 적용되었는지 확인
            allImprovedCodesApplied &= Objects.equals(
                    pushCodes.get(analysis.getId()), latestFile.content());
        }

        // 원본 파일이 변경된 경우 처리
        if (!allSourcesUnchanged) {
            // 이미 개선 코드가 적용된 경우 Push 상태만 복구
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
            // 덮어쓰기가 허용되지 않는 경우 중단
            if (!request.overwriteChangedFiles()) {
                throw new CustomException(ErrorCode.SOURCE_CHANGED_SINCE_ANALYSIS);
            }
        }

        // 현재 Commit의 Tree SHA 조회
        String baseTreeSha = githubService.getCommitTreeSha(
                userId, repo.getOrganization(), repo.getName(), headSha);
        // 각 파일의 GitHub 권한 정보 조회
        Map<String, String> fileModes = githubService.getFileModes(
                userId,
                repo.getOrganization(),
                repo.getName(),
                baseTreeSha,
                filePaths);

        List<BatchPushResponse.FileBlob> files = new ArrayList<>();
        List<GithubTreeItem> treeItems = new ArrayList<>();
        // 개선 코드 Blob 생성 및 TreeItem 준비
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
        // 새로운 Git Tree 생성
        String preparedTreeSha = githubService.createTree(
                userId, repo.getOrganization(), repo.getName(), baseTreeSha, treeItems);
        // Commit 메시지 생성
        String commitMessage = "GuardrAil: AI 코드 개선 반영 (" + analyses.size() + "개 파일)";
        // 새로운 Commit 생성
        String commitSha = githubService.createCommit(
                userId,
                repo.getOrganization(),
                repo.getName(),
                commitMessage,
                preparedTreeSha,
                headSha);
        // 브랜치 HEAD를 새 Commit으로 변경
        githubService.updateBranchHead(
                userId, repo.getOrganization(), repo.getName(), branch, commitSha);

        LocalDateTime pushedAt = LocalDateTime.now();
        // 각 분석에 Push 정보 저장
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

    //여러 분석 결과를 기반으로 Batch Pull Request 생성 
    @Transactional
    public BatchPullRequestResponse createBatchPullRequest(
            BatchPullRequestRequest request, Integer userId) {
        List<Integer> ids = request.analysisIds();
        // 중복된 분석 ID 확인
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new CustomException(ErrorCode.BATCH_ANALYSIS_DUPLICATED);
        }

        // 요청된 분석 정보 조회
        Map<Integer, Analysis> found = analysisRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Analysis::getId, Function.identity()));
        // 요청된 분석 ID 중 일부가 존재하지 않는 경우 예외 처리
        if (found.size() != ids.size()) {
            throw new CustomException(ErrorCode.ANALYSIS_NOT_FOUND);
        }

        List<Analysis> analyses = ids.stream().map(found::get).toList();
        Analysis first = analyses.get(0);
        GithubRepo repo = first.getGithubRepo();
        Integer repoId = repo.getId();
        String headBranch = first.getBranch();
        String pushedCommitSha = first.getPushedCommitSha();

        // PR 생성 대상 분석 검증
        for (Analysis analysis : analyses) {
            // 사용자 권한 확인
            validateOwner(analysis, userId);
            /*
            브랜치 정보, 저장소 경로, 동일 브랜치 등 생성시 다양한 정보 확인 
             */
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

        // 요청한 Base 브랜치가 없으면 저장소의 기본 브랜치 사용
        String baseBranch = StringUtils.hasText(request.baseBranch())
                ? request.baseBranch().trim()
                : githubService.getDefaultBranch(
                        userId, repo.getOrganization(), repo.getName());
        // 현재 head 브랜치와 Base브랜치가 동일한지 검증 
        if (headBranch.equals(baseBranch)) {
            throw new CustomException(ErrorCode.GITHUB_PR_SAME_BRANCH);
        }

        // 현재 Head 브랜치의 최신 Commit 확인
        String currentHeadSha = githubService.getBranchHeadSha(
                userId, repo.getOrganization(), repo.getName(), headBranch);
        if (!pushedCommitSha.equals(currentHeadSha)) {
            throw new CustomException(ErrorCode.GITHUB_PR_HEAD_MISMATCH);
        }

        // 요청값이 없으면 기본 PR 제목과 본문 사용
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
            // 이미 열려 있는 PR이 아닌 경우 예외 처리
            if (e.getErrorCode() != ErrorCode.GITHUB_PR_ALREADY_OPEN) {
                throw e;
            }
            // 동일 브랜치의 기존 PR 조회
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

        // PR의 Head Commit이 Push된 Commit과 동일한지 확인
        if (!pushedCommitSha.equals(result.headCommitSha())) {
            // 새로 생성된 잘못된 PR은 종료되도록 설정
            if (!recovered) {
                githubService.closePullRequest(
                        userId,
                        repo.getOrganization(),
                        repo.getName(),
                        result.number());
            }
            throw new CustomException(ErrorCode.GITHUB_PR_HEAD_MISMATCH);
        }

        // PR 정보 조회 또는 DB 저장
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
        
        // PR과 각 분석 결과 연결
        pullRequestAnalysisRepository.saveAll(analyses.stream()
                .map(analysis -> PullRequestAnalysis.builder()
                        .pullRequest(pullRequest)
                        .analysis(analysis)
                        .build())
                .toList());

        // 생성된 Batch PR 정보 반환
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

    //Batch Pull Request의 기본 본문 생성
    private String buildBatchPullRequestBody(List<Analysis> analyses) {
        return "선택한 AI 개선 코드 " + analyses.size() + "개를 반영합니다.\n\n"
                + analyses.stream()
                        .map(analysis -> "- `" + analysis.getFilePath() + "`")
                        .collect(Collectors.joining("\n"));
    }

    //단일 분석 결과를 기반으로 GitHub Pull Request 생성
    @Transactional
    public String createPullRequest(Integer analysisId, Integer userId, String baseBranch,
                                    String requestTitle, String requestBody) {  // 파라미터 2개 추가
        // 분석 정보 조회
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_NOT_FOUND));

        // 사용자 권한 확인
        if (!analysis.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
        // 완료된 분석만 PR 생성 가능
        if (!"COMPLETED".equals(analysis.getStatus())) {
            throw new CustomException(ErrorCode.ANALYSIS_NOT_COMPLETED);
        }
        // 브랜치 정보 확인
        if (analysis.getBranch() == null) {
            throw new CustomException(ErrorCode.ANALYSIS_BRANCH_INFO_MISSING);
        }
        // 이미 PR이 생성된 분석인지 확인
        if (pullRequestAnalysisRepository.existsByAnalysis_Id(analysisId)) {
            throw new CustomException(ErrorCode.GITHUB_PR_ALREADY_CREATED);
        }

        // 분석 결과 조회
        Finding finding = findingRepository.findByAnalysisId(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.FINDING_NOT_FOUND));

        GithubRepo repo = analysis.getGithubRepo();
        // Base 브랜치가 없으면 저장소의 기본 브랜치 사용
        String targetBranch = (baseBranch != null && !baseBranch.isBlank())
                ? baseBranch
                : githubService.getDefaultBranch(userId, repo.getOrganization(), repo.getName());

        // Head와 Base 브랜치가 동일한지 확인
        if (analysis.getBranch().equals(targetBranch)) {
            throw new CustomException(ErrorCode.GITHUB_PR_SAME_BRANCH);
        }

        // 사용자가 모달에서 값을 보냈으면 그것을, 비어 있으면 기본값을 사용
        // hasText(): null / "" / "   " 를 한번에 체크, trim()으로 공백 제거
        String title = StringUtils.hasText(requestTitle)
                ? requestTitle.trim()
                : buildDefaultTitle(analysis, finding);
        String body = StringUtils.hasText(requestBody)
                ? requestBody.trim()
                : buildDefaultBody(analysis, finding, analysisId);

        // GitHub Pull Request 생성
        GithubPullRequestResult result = githubService.createPullRequest(
                userId, repo.getOrganization(), repo.getName(), analysis.getBranch(), targetBranch, title, body);

        // 생성된 PR 정보 DB 저장
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
        // PR과 분석 결과 연결
        pullRequestAnalysisRepository.save(
                PullRequestAnalysis.builder()
                        .pullRequest(pullRequest)
                        .analysis(analysis)
                        .build()
        );

        return result.url();
    }

    /**
     * 사용자가 제목을 지정하지 않았을 때 쓰는 기본 PR 제목 생성되도록 설정 
     */
    private String buildDefaultTitle(Analysis analysis, Finding finding) {
        String fileName = analysis.getFilePath() != null
                ? analysis.getFilePath().substring(analysis.getFilePath().lastIndexOf('/') + 1)
                : "코드";
        return "GuardrAil: " + fileName + " 코드 개선 (이슈 " + finding.getTotalIssues() + "건)";
    }
    /**
     * 사용자가 설명을 지정하지 않았을 때 쓰는 기본 PR 본문 생성 
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

        // 코드가 null이거나 공백이면 개선 가능률 0으로 반환
        if (originCode == null || originCode.isBlank()){
            return zero;
        }

        // 개선 코드가 없으면 계산 불가
        if (modifiedCode == null || modifiedCode.isBlank()) {
            return null;
        }

        // 원본 코드 전체 줄 수 계산
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
        // 원본 코드에서 변경되거나 제거된 줄 계산
        for (String line : originLines) {
            Integer remaining = modifiedCounts.get(line);
            if (remaining != null && remaining > 0) {
                modifiedCounts.put(line, remaining - 1);
            } else {
                changedLines++;
            }
        }

        // 변경된 코드가 없는 경우 개선 가능률 0으로 반환
        if (changedLines == 0) {
            return zero;
        }

        // 전체 코드 대비 변경 비율 계산
        BigDecimal ratio = BigDecimal.valueOf(changedLines)
                .divide(BigDecimal.valueOf(totalLines), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);

        // 개선 가능률이 100%를 초과하지 않도록 제한
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

    //기존 프롬프트와 분석 결과를 기반으로 프롬프트 재구성
    public PromptReconstructApiResponse reconstructPrompt(Integer analysisId, String originalPrompt) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다."));

        Finding finding = findingRepository.findByAnalysisId(analysisId)
                .orElseThrow(() -> new IllegalArgumentException("이 분석에 대한 발견 결과가 없습니다."));
        
        // 사용자가 입력한 프롬프트 저장 
        analysis.setPrompt(originalPrompt);
        analysisRepository.save(analysis);
        
        try {
            // 분석 결과를 JSON 형태로 반환  
            List<Map<String, Object>> vulnerabilities = jsonMapper.readValue(
                    finding.getSecuResult(), new TypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> complexityDetails = jsonMapper.readValue(
                    finding.getInefficiencyResult(), new TypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> duplicateSnippets = jsonMapper.readValue(
                finding.getDuplicateResult(), new TypeReference<List<Map<String, Object>>>() {});
                
            // AI 서버에 전달할 프롬프트 재구성 요청 생성
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

            // AI 서버에 프롬프트 재구성 요청
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
