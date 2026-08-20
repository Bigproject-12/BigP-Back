package com.aivle.bigproject.service;

import com.aivle.bigproject.ai.dto.IndexFileItem;
import com.aivle.bigproject.ai.service.EmbeddingService;
import org.springframework.scheduling.annotation.Async;

import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.repository.GithubRepoRepository;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.UserRepository;
import com.aivle.bigproject.security.GithubTokenCrypto;
import com.aivle.bigproject.entity.UserRepo;
import com.aivle.bigproject.repository.UserRepoRepository;
import com.aivle.bigproject.dto.repo.RepoResponse;
import com.aivle.bigproject.repository.RepoEmbeddingRepository; 
import com.aivle.bigproject.dto.repo.BranchResponse;
import com.aivle.bigproject.dto.repo.GithubPullRequestResult;
import com.aivle.bigproject.dto.repo.GithubPullRequestResponse;
import com.aivle.bigproject.dto.repo.RepoTreeResponse;
import com.aivle.bigproject.dto.repo.GithubFileContent;
import com.aivle.bigproject.dto.repo.GithubTreeItem;
import com.aivle.bigproject.repository.GithubPullRequestRepository;
import com.aivle.bigproject.repository.AnalysisRepository;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
//GitHub 저장소 연동, Webhook 등록 및 코드 임베딩 처리를 담당하는 Service
@Slf4j
@Service
@Transactional(readOnly = true)
public class GithubService {

    private final UserRepository userRepository;
    private final GithubTokenCrypto githubTokenCrypto;
    private final GithubRepoRepository githubRepoRepository;
    private final UserRepoRepository userRepoRepository;
    private final String webhookCallbackUrl;
    private final String webhookSecret;
    private final EmbeddingService embeddingService;
    private final RepoEmbeddingRepository repoEmbeddingRepository;
    private final GithubPullRequestRepository githubPullRequestRepository;
    private final AnalysisRepository analysisRepository;
    private final JsonMapper jsonMapper;
    // 임베딩 대상으로 허용하는 코드 파일 확장자
    private static final Set<String> EMBEDDABLE_EXTENSIONS = Set.of(
            ".java", ".py", ".js", ".jsx", ".ts", ".tsx"
    );

    public GithubService(UserRepository userRepository, GithubTokenCrypto githubTokenCrypto, GithubRepoRepository githubRepoRepository, EmbeddingService embeddingService, UserRepoRepository userRepoRepository, RepoEmbeddingRepository repoEmbeddingRepository, GithubPullRequestRepository githubPullRequestRepository, AnalysisRepository analysisRepository, JsonMapper jsonMapper, @Value("${github.webhook-callback-url}") String webhookCallbackUrl,
        @Value("${github.webhook-secret}") String webhookSecret) {
        this.userRepository = userRepository;
        this.githubTokenCrypto = githubTokenCrypto;
        this.githubRepoRepository = githubRepoRepository;
        this.userRepoRepository = userRepoRepository;
        this.embeddingService = embeddingService;
        this.repoEmbeddingRepository = repoEmbeddingRepository;
        this.githubPullRequestRepository = githubPullRequestRepository;
        this.analysisRepository = analysisRepository;
        this.jsonMapper = jsonMapper;
        this.webhookCallbackUrl = webhookCallbackUrl;
        this.webhookSecret = webhookSecret;
    }
    /** 
     * GitHub Organization의 저장소 목록을 조회하고 플랫폼에 연결
     * 신규 저장소는 Webhook 등록 및 코드 임베딩 수행
    */
    @Transactional
    public Object connectAndFetchRepos(Integer userId, String orgName) {
        User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        //// GitHub OAuth 연동 여부 확인
        if (user.getGithubAccessToken() == null) {
            throw new IllegalArgumentException("연동된 GitHub 토큰이 없습니다. OAuth 인증을 먼저 진행해 주세요.");
        }
        // 암호화된 GitHub Access Token 복호화
        String tokenToUse = githubTokenCrypto.decrypt(user.getGithubAccessToken());

        RestTemplate restTemplate = new RestTemplate();
        // GitHub API 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenToUse); 
        headers.set("Accept", "application/vnd.github+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        // Organization의 저장소 목록 조회 URL
        String url = "https://api.github.com/orgs/" + orgName + "/repos?per_page=100&sort=updated";

        try {
            // GitHub API에서 Organization 저장소 목록 조회
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> repoList = (List<Map<String, Object>>) response.getBody();

            if (repoList != null) {
                // 조회된 저장소 목록 순회
                for (Map<String, Object> repoData : repoList) {
                    String repoName = (String) repoData.get("name");
                    String repoUrl = (String) repoData.get("html_url");
                    String language = (String) repoData.get("language");
                    String lastUpdated = (String) repoData.get("updated_at");
                    String organization = orgName;
                    Boolean isPrivate = (Boolean) repoData.get("private");

                    // DB에 없는 새로운 레포지토리일 경우에만 Insert
                    GithubRepo currentRepo = githubRepoRepository.findByName(repoName)
                            .orElseGet(() -> {
                                GithubRepo newRepo = GithubRepo.builder()
                                        .name(repoName)
                                        .repoUrl(repoUrl)
                                        .language(language)
                                        .lastUpdated(lastUpdated)
                                        .isPrivate(isPrivate)
                                        .organization(organization)
                                        .createdAt(LocalDate.now())
                                        .build();
                                GithubRepo saved = githubRepoRepository.save(newRepo);
                                // 신규 저장소 Webhook 등록
                                registerWebhook(saved, tokenToUse);
                                // 저장소 코드 조회 및 임베딩 생성
                                fetchAndEmbedRepoFiles(saved.getId(), organization, repoName, "dev", tokenToUse);
                                return saved;
                            });
                    // 기존 저장소에 Webhook이 없으면 등록
                    if (currentRepo.getWebhookId() == null) {
                        registerWebhook(currentRepo, tokenToUse);
                    }
                    // 기존 임베딩이 없으면 저장소 코드 임베딩 생성
                    if (!repoEmbeddingRepository.existsByGithubRepo_Id(currentRepo.getId())) {
                        fetchAndEmbedRepoFiles(currentRepo.getId(), organization, repoName, "dev", tokenToUse);
                    }

                    // 사용자와 저장소가 아직 연결되지 않은 경우 UserRepo 생성
                    if (!userRepoRepository.existsByUserAndGithubRepo(user, currentRepo)) {
                        UserRepo userRepo = UserRepo.builder()
                                .user(user)             
                                .githubRepo(currentRepo) 
                                .createdAt(LocalDate.now())
                                .build();
                        
                        userRepoRepository.save(userRepo);
                    }
                    else{
                        System.out.println("이미 연동한 적 있는 repo라 DB에 안 넣겠다.");
                    }
                }
            }

            return response.getBody();
        } catch (Exception e) {
            // GitHub Organization 연동 실패 로그 기록
            log.error("GitHub 조직 연동 실패 (orgName={}): {}", orgName, e.getMessage(), e);
            throw new IllegalArgumentException("GitHub 연동에 실패했습니다. 올바른 조직명과 권한이 있는 토큰인지 확인해주세요.");
        }
    }

    //저장소의 전체 파일 트리 조회
    public RepoTreeResponse getRepositoryTree(Integer userId, Integer repoId, String branch) {
        return getRepositoryTree(userId, repoId, branch, false);
    }

    /** 저장소의 파일 트리 조회 
     * issuesOnly가 true이면 이슈가 존재하는 파일만 반환 
     */
    public RepoTreeResponse getRepositoryTree(
            Integer userId,
            Integer repoId,
            String branch,
            boolean issuesOnly
    ) {
        // 유효한 브랜치명인지 확인
        if (branch == null || branch.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_BRANCH);
        }
        // 사용자가 해당 저장소에 연결되어 있는지 확인
        UserRepo userRepo = userRepoRepository.findByUser_IdAndGithubRepo_Id(userId, repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));
        User user = userRepo.getUser();
        // GitHub Access Token 연동 여부 확인
        if (user.getGithubAccessToken() == null) {
            throw new CustomException(ErrorCode.GITHUB_TOKEN_NOT_CONNECTED);
        }
        GithubRepo repo = userRepo.getGithubRepo();
        // GitHub API 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubTokenCrypto.decrypt(user.getGithubAccessToken()));
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2026-03-10");

        // 브랜치명에 '/'가 포함된 경우 git/tree api 경로에서 문제가 발생할수 있으며.,
        // 그 경우에만 커밋 SHA로 변환해서 쓴다 (매번 변환하면 GitHub API 왕복 횟수 증가 ).
        String branchSha = branch.contains("/") ? resolveBranchSha(repo, branch, headers) : branch;
        // GitHub Tree API 요청 URL 생성
        String url = UriComponentsBuilder.fromUriString("https://api.github.com")
                .pathSegment("repos", repo.getOrganization(), repo.getName(), "git", "trees", branchSha)
                .queryParam("recursive", 1)
                .build()
                .encode()
                .toUriString();

        try {
            // GitHub API를 통해 저장소 파일 트리 조회
            ResponseEntity<Map<String, Object>> response = restTemplate().exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );
            Map<String, Object> body = response.getBody();
            // GitHub API 응답이 없는 경우 예외 처리
            if (body == null) {
                throw new CustomException(ErrorCode.GITHUB_API_ERROR);
            }
            boolean truncated = Boolean.TRUE.equals(body.get("truncated"));
            Object rawTree = body.get("tree");
            // GitHub Tree 응답을 Map 목록으로 변환
            List<Map<String, Object>> tree = rawTree instanceof List<?> values
                    ? values.stream()
                            .filter(Map.class::isInstance)
                            .map(value -> (Map<String, Object>) value)
                            .toList()
                    : Collections.emptyList();
            if (truncated) {
                log.warn("{}/{} 레포의 {} 브랜치 트리를 재귀 조회로 보완합니다.",
                        repo.getOrganization(), repo.getName(), branch);
                TreeFetch completeTree = fetchCompleteTree(repo, branchSha, headers);
                tree = completeTree.items();
                truncated = completeTree.truncated();
            }
            Map<String, IssueBadge> issueBadges = issueBadges(userId, repoId, branch);
            List<RepoTreeResponse.Item> items = tree.stream()
                    .filter(item -> !issuesOnly
                            || issueBadges.getOrDefault((String) item.get("path"), IssueBadge.EMPTY)
                                    .total() > 0)
                    .map(item -> {
                        String path = (String) item.get("path");
                        IssueBadge issues = issueBadges.getOrDefault(path, IssueBadge.EMPTY);
                        return new RepoTreeResponse.Item(
                                path,
                                (String) item.get("type"),
                                (String) item.get("sha"),
                                item.get("size") instanceof Number size ? size.longValue() : null,
                                issues.total(),
                                issues.security(),
                                issues.inefficiency(),
                                issues.other(),
                                issues.analyzed(),
                                issues.analysisId(),
                                issues.lastAnalyzedAt(),
                                issues.qualityScore(),
                                issues.critical(),
                                issues.high(),
                                issues.medium(),
                                issues.low());
                    })
                    .toList();
            return new RepoTreeResponse(repoId, branch, truncated, items);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new CustomException(ErrorCode.REPO_NOT_FOUND);
        } catch (HttpClientErrorException.Forbidden exception) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        } catch (RestClientException exception) {
            log.error("GitHub 파일 트리 조회 실패 (repoId={}, branch={}): {}",
                    repoId, branch, exception.getMessage());
            throw new CustomException(ErrorCode.GITHUB_API_ERROR);
        }
    }

    // 브랜치명(예: "feature/OSH")을 커밋 SHA로 변환한다. GitHub의 branches/{branch} 엔드포인트는
    // '/'가 포함된 브랜치명을 %2F로 인코딩해도 정상 처리하지만, git/trees/{ref} 쪽은 그렇지 않다.
    private String resolveBranchSha(GithubRepo repo, String branch, HttpHeaders headers) {
        String url = UriComponentsBuilder.fromUriString("https://api.github.com")
                .pathSegment("repos", repo.getOrganization(), repo.getName(), "branches", branch)
                .build()
                .encode()
                .toUriString();
        try {
            Map<String, Object> body = restTemplate().exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            ).getBody();
            Object commit = body != null ? body.get("commit") : null;
            if (!(commit instanceof Map<?, ?> commitMap) || !(commitMap.get("sha") instanceof String sha)) {
                throw new CustomException(ErrorCode.GITHUB_API_ERROR);
            }
            return sha;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new CustomException(ErrorCode.INVALID_BRANCH);
        }
    }

    private TreeFetch fetchCompleteTree(GithubRepo repo, String branch, HttpHeaders headers) {
        List<Map<String, Object>> items = new ArrayList<>();
        boolean truncated = appendTree(repo, branch, "", headers, items);
        return new TreeFetch(items, truncated);
    }

    private boolean appendTree(
            GithubRepo repo,
            String ref,
            String parentPath,
            HttpHeaders headers,
            List<Map<String, Object>> result
    ) {
        String url = UriComponentsBuilder.fromUriString("https://api.github.com")
                .pathSegment("repos", repo.getOrganization(), repo.getName(), "git", "trees", ref)
                .build()
                .encode()
                .toUriString();
        Map<String, Object> body = restTemplate().exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, Object>>() {})
                .getBody();
        if (body == null) {
            throw new CustomException(ErrorCode.GITHUB_API_ERROR);
        }
        boolean truncated = Boolean.TRUE.equals(body.get("truncated"));
        for (Map<String, Object> item : treeItems(body.get("tree"))) {
            String path = parentPath.isEmpty()
                    ? (String) item.get("path")
                    : parentPath + "/" + item.get("path");
            Map<String, Object> fullPathItem = new HashMap<>(item);
            fullPathItem.put("path", path);
            result.add(fullPathItem);
            if ("tree".equals(item.get("type")) && item.get("sha") instanceof String sha) {
                truncated |= appendTree(repo, sha, path, headers, result);
            }
        }
        return truncated;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> treeItems(Object rawTree) {
        return rawTree instanceof List<?> values
                ? values.stream()
                        .filter(Map.class::isInstance)
                        .map(value -> (Map<String, Object>) value)
                        .toList()
                : Collections.emptyList();
    }

    private Map<String, IssueBadge> issueBadges(Integer userId, Integer repoId, String branch) {
        Map<String, IssueBadge> badges = new java.util.HashMap<>();
        for (AnalysisRepository.FileIssueSummary summary
                : analysisRepository.findLatestFileIssues(userId, repoId, branch)) {
            IssueBadge badge = IssueBadge.file(summary, criticalIssues(summary));
            String path = summary.getFilePath();
            badges.merge(path, badge, IssueBadge::add);
            IssueBadge folderBadge = badge.asFolder();
            for (int slash = path.indexOf('/'); slash >= 0; slash = path.indexOf('/', slash + 1)) {
                badges.merge(path.substring(0, slash), folderBadge, IssueBadge::add);
            }
        }
        return badges;
    }

    private int criticalIssues(AnalysisRepository.FileIssueSummary summary) {
        if (summary.getInefficiencyResult() == null) {
            return 0;
        }
        try {
            List<Map<String, Object>> issues = jsonMapper.readValue(
                    summary.getInefficiencyResult(),
                    new TypeReference<List<Map<String, Object>>>() {});
            return (int) issues.stream()
                    .map(issue -> issue.get("complexity_score"))
                    .filter(Number.class::isInstance)
                    .map(Number.class::cast)
                    .filter(score -> score.intValue() >= 25)
                    .count();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private record IssueBadge(
            int total,
            int security,
            int inefficiency,
            Integer analysisId,
            LocalDateTime lastAnalyzedAt,
            double qualitySum,
            int analyzedFileCount,
            int critical,
            int high,
            int medium,
            int low
    ) {
        private static final IssueBadge EMPTY = new IssueBadge(
                0, 0, 0, null, null, 0, 0, 0, 0, 0, 0);

        private static IssueBadge file(
                AnalysisRepository.FileIssueSummary summary,
                int critical
        ) {
            int total = summary.getTotalIssueCount();
            int security = summary.getSecurityIssueCount();
            int inefficiency = summary.getInefficiencyIssueCount();
            int other = Math.max(0, total - security - inefficiency);
            double quality = Math.max(0, 100 - security * 10 - inefficiency * 5 - other * 3);
            return new IssueBadge(
                    total, security, inefficiency,
                    summary.getAnalysisId(), summary.getAnalyzedAt(), quality, 1,
                    critical, security + Math.max(0, inefficiency - critical), other, 0);
        }

        private IssueBadge add(IssueBadge other) {
            return new IssueBadge(
                    total + other.total,
                    security + other.security,
                    inefficiency + other.inefficiency,
                    analysisId,
                    latest(lastAnalyzedAt, other.lastAnalyzedAt),
                    qualitySum + other.qualitySum,
                    analyzedFileCount + other.analyzedFileCount,
                    critical + other.critical,
                    high + other.high,
                    medium + other.medium,
                    low + other.low);
        }

        private IssueBadge asFolder() {
            return new IssueBadge(
                    total, security, inefficiency, null, lastAnalyzedAt,
                    qualitySum, analyzedFileCount, critical, high, medium, low);
        }

        private int other() {
            return Math.max(0, total - security - inefficiency);
        }

        private boolean analyzed() {
            return analyzedFileCount > 0;
        }

        private Double qualityScore() {
            return analyzed() ? Math.round(qualitySum / analyzedFileCount * 10) / 10.0 : null;
        }

        private static LocalDateTime latest(LocalDateTime first, LocalDateTime second) {
            if (first == null) return second;
            if (second == null) return first;
            return first.isAfter(second) ? first : second;
        }
    }

    private record TreeFetch(List<Map<String, Object>> items, boolean truncated) {}

    public GithubFileContent getLatestFileContent(
            Integer userId,
            Integer repoId,
            String filePath,
            String branch
    ) {
        return getLatestFileContent(userId, repoId, filePath, branch, false);
    }

    // treatMissingAsNull=true: 해당 경로에 파일이 아직 없는 경우(새 파일 생성 흐름) null을 반환하고,
    // 그 외 오류(권한 없음, GitHub 응답 이상 등)는 기존과 동일하게 예외 처리
    public GithubFileContent getLatestFileContent(
            Integer userId,
            Integer repoId,
            String filePath,
            String branch,
            boolean treatMissingAsNull
    ) {
        if (branch == null || branch.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_BRANCH);
        }
        if (filePath == null || filePath.isBlank()) {
            throw new CustomException(ErrorCode.ANALYSIS_BRANCH_FILE_INFO_MISSING);
        }
        UserRepo userRepo = userRepoRepository.findByUser_IdAndGithubRepo_Id(userId, repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));
        User user = userRepo.getUser();
        if (user.getGithubAccessToken() == null) {
            throw new CustomException(ErrorCode.GITHUB_TOKEN_NOT_CONNECTED);
        }
        GithubRepo repo = userRepo.getGithubRepo();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubTokenCrypto.decrypt(user.getGithubAccessToken()));
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2026-03-10");
        String url = "https://api.github.com/repos/" + repo.getOrganization() + "/" + repo.getName()
                + "/contents/" + UriUtils.encodePath(filePath, StandardCharsets.UTF_8)
                + "?ref=" + UriUtils.encodeQueryParam(branch, StandardCharsets.UTF_8);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate().exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );
            Map<String, Object> body = response.getBody();
            if (body == null || body.get("content") == null || body.get("sha") == null
                    || !"base64".equals(body.get("encoding"))) {
                throw new CustomException(ErrorCode.GITHUB_FILE_FETCH_FAILED);
            }
            String content = new String(
                    Base64.getMimeDecoder().decode((String) body.get("content")),
                    StandardCharsets.UTF_8);
            return new GithubFileContent(content, (String) body.get("sha"));
        } catch (CustomException exception) {
            throw exception;
        } catch (HttpClientErrorException.NotFound exception) {
            if (treatMissingAsNull) {
                return null;
            }
            throw new CustomException(ErrorCode.GITHUB_FILE_FETCH_FAILED);
        } catch (HttpClientErrorException.Forbidden exception) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        } catch (RestClientException | IllegalArgumentException exception) {
            log.error("GitHub 최신 파일 조회 실패 (repoId={}, path={}, branch={}): {}",
                    repoId, filePath, branch, exception.getMessage());
            throw new CustomException(ErrorCode.GITHUB_FILE_FETCH_FAILED);
        }
    }

    public List<RepoResponse> getUserRepos(Integer userId) {
        List<UserRepo> userRepos = userRepoRepository.findAllByUserId(userId);

        return userRepos.stream()
                .map(userRepo -> RepoResponse.from(userRepo.getGithubRepo()))
                .toList();
    }
    public RepoResponse getUserRepo(Integer userId, Integer repoId) {
        UserRepo userRepo = userRepoRepository
                .findByUser_IdAndGithubRepo_Id(userId, repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));

        return RepoResponse.from(userRepo.getGithubRepo());
    }

    public List<BranchResponse> getRepositoryBranches(Integer userId, Integer repoId) {
        UserRepo userRepo = userRepoRepository
                .findByUser_IdAndGithubRepo_Id(userId, repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));
        User user = userRepo.getUser();
        if (user.getGithubAccessToken() == null) {
            throw new CustomException(ErrorCode.GITHUB_TOKEN_NOT_CONNECTED);
        }

        GithubRepo repo = userRepo.getGithubRepo();
        String defaultBranch = getDefaultBranch(userId, repo.getOrganization(), repo.getName());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubTokenCrypto.decrypt(user.getGithubAccessToken()));
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2026-03-10");
        String url = "https://api.github.com/repos/" + repo.getOrganization() + "/" + repo.getName() + "/branches?per_page=100";

        try {
            ResponseEntity<List<Map<String, Object>>> response = new RestTemplate().exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );
            List<Map<String, Object>> branches = response.getBody() == null
                    ? Collections.emptyList()
                    : response.getBody();

            return branches.stream()
                    .map(branch -> {
                        Map<String, Object> commit = (Map<String, Object>) branch.get("commit");
                        String name = (String) branch.get("name");
                        return new BranchResponse(
                                name,
                                commit == null ? null : (String) commit.get("sha"),
                                Boolean.TRUE.equals(branch.get("protected")),
                                name.equals(defaultBranch)
                        );
                    })
                    .toList();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new CustomException(ErrorCode.REPO_NOT_FOUND);
        } catch (HttpClientErrorException.Forbidden exception) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        } catch (RestClientException exception) {
            log.error("GitHub 브랜치 조회 실패 (repoId={}): {}", repoId, exception.getMessage());
            throw new CustomException(ErrorCode.GITHUB_API_ERROR);
        }
    }
    public List<GithubPullRequestResponse> getRepositoryPullRequests(
            Integer userId,
            Integer repoId,
            String scope,
            String state,
            int page,
            int size
    ) {
        String normalizedScope = scope.toLowerCase(Locale.ROOT);
        String normalizedState = state.toLowerCase(Locale.ROOT);
        if ((!"mine".equals(normalizedScope) && !"all".equals(normalizedScope))
                || (!"open".equals(normalizedState)
                    && !"closed".equals(normalizedState)
                    && !"all".equals(normalizedState))
                || page < 1 || size < 1 || size > 50) {
            throw new CustomException(ErrorCode.INVALID_PR_QUERY);
        }

        UserRepo userRepo = userRepoRepository
                .findByUser_IdAndGithubRepo_Id(userId, repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));
        User user = userRepo.getUser();
        if (user.getGithubAccessToken() == null) {
            throw new CustomException(ErrorCode.GITHUB_TOKEN_NOT_CONNECTED);
        }

        GithubRepo repo = userRepo.getGithubRepo();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubTokenCrypto.decrypt(user.getGithubAccessToken()));
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2026-03-10");
        String url = "https://api.github.com/repos/" + repo.getOrganization() + "/" + repo.getName()
                + "/pulls?state=" + normalizedState + "&page=" + page + "&per_page=" + size;

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate().exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );
            List<Map<String, Object>> pullRequests = response.getBody() == null
                    ? Collections.emptyList()
                    : response.getBody();
            List<Map<String, Object>> visiblePullRequests = pullRequests.stream()
                    .filter(pr -> "all".equals(normalizedScope)
                            || user.getGitName() != null
                            && user.getGitName().equalsIgnoreCase(nestedString(pr, "user", "login")))
                    .toList();
            Set<Integer> githubPrNumbers = visiblePullRequests.stream()
                    .map(pr -> ((Number) pr.get("number")).intValue())
                    .collect(Collectors.toSet());
            Set<Integer> platformGeneratedNumbers = githubPrNumbers.isEmpty()
                    ? Collections.emptySet()
                    : githubPullRequestRepository.findPlatformGeneratedNumbers(repoId, githubPrNumbers);

            return visiblePullRequests.stream()
                    .map(pr -> toPullRequestResponse(pr, platformGeneratedNumbers))
                    .toList();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new CustomException(ErrorCode.REPO_NOT_FOUND);
        } catch (HttpClientErrorException.Forbidden exception) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        } catch (RestClientException exception) {
            log.error("GitHub PR 조회 실패 (repoId={}): {}", repoId, exception.getMessage());
            throw new CustomException(ErrorCode.GITHUB_API_ERROR);
        }
    }

    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    private GithubPullRequestResponse toPullRequestResponse(
            Map<String, Object> pullRequest,
            Set<Integer> platformGeneratedNumbers
    ) {
        Integer number = ((Number) pullRequest.get("number")).intValue();
        OffsetDateTime mergedAt = parseDateTime((String) pullRequest.get("merged_at"));
        String githubState = (String) pullRequest.get("state");
        String status = "open".equalsIgnoreCase(githubState)
                ? "OPEN"
                : mergedAt != null ? "MERGED" : "CLOSED";

        return new GithubPullRequestResponse(
                number,
                (String) pullRequest.get("title"),
                nestedString(pullRequest, "user", "login"),
                status,
                (String) pullRequest.get("html_url"),
                nestedString(pullRequest, "head", "ref"),
                nestedString(pullRequest, "base", "ref"),
                parseDateTime((String) pullRequest.get("created_at")),
                parseDateTime((String) pullRequest.get("updated_at")),
                mergedAt,
                platformGeneratedNumbers.contains(number)
        );
    }

    private String nestedString(Map<String, Object> source, String objectKey, String valueKey) {
        Object nested = source.get(objectKey);
        return nested instanceof Map<?, ?> values ? (String) values.get(valueKey) : null;
    }

    private OffsetDateTime parseDateTime(String value) {
        return value == null ? null : OffsetDateTime.parse(value);
    }

    private void registerWebhook(GithubRepo repo, String token) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");

        Map<String, Object> config = Map.of(
                "url", webhookCallbackUrl,   // application.yml의 github.webhook-callback-url
                "content_type", "json",
                "secret", webhookSecret       // application.yml의 github.webhook-secret
        );
        Map<String, Object> body = Map.of(
                "name", "web",
                "active", true,
                "events", List.of("push", "pull_request"),
                "config", config
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = "https://api.github.com/repos/" + repo.getOrganization() + "/" + repo.getName() + "/hooks";

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Long hookId = Long.valueOf(response.getBody().get("id").toString());
            repo.setWebhookId(hookId);
            repo.setWebhookActive(true);
            log.info("{} 레포 Webhook 등록 성공 (hookId={})", repo.getName(), hookId);
        } catch (Exception e) {
            log.error("{} 레포 Webhook 등록 실패: {}", repo.getName(), e.getMessage());
        }
    }

    @Transactional
    public void registerOrgWebhook(String orgName, String token) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");

        Map<String, Object> config = Map.of(
                "url", webhookCallbackUrl,
                "content_type", "json",
                "secret", webhookSecret
        );
        Map<String, Object> body = Map.of(
                "name", "web",
                "active", true,
                "events", List.of("organization"),
                "config", config
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = "https://api.github.com/orgs/" + orgName + "/hooks";

        try {
            restTemplate.postForEntity(url, entity, Map.class);
            log.info("{} 조직 Webhook 등록 성공", orgName);
        } catch (Exception e) {
            log.error("{} 조직 Webhook 등록 실패 (Owner 권한 필요): {}", orgName, e.getMessage());
        }
    }

    @Transactional
    public void revokeOrgAccess(String orgName, String gitId) {
        userRepository.findByGitId(gitId).ifPresentOrElse(user -> {
            userRepoRepository.deleteByUserIdAndOrganization(user.getId(), orgName);
            log.info("{} 님이 {} 조직에서 추방되어, 해당 조직 레포 접근 권한을 삭제했습니다.", user.getGitName(), orgName);
        }, () -> {
            log.warn("gitId={} 은(는) 우리 서비스에 등록된 사용자가 아닙니다.", gitId);
        });
    }

    @Transactional
    public void triggerOrgWebhookRegistration(Integer userId, String orgName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String token = githubTokenCrypto.decrypt(user.getGithubAccessToken());
        registerOrgWebhook(orgName, token);
    }

    public String getFileSha(Integer userId, String orgName, String repoName, String filePath, String branch) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String token = githubTokenCrypto.decrypt(user.getGithubAccessToken());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://api.github.com/repos/" + orgName + "/" + repoName + "/contents/" + filePath + "?ref=" + branch;

        try { ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return (String) response.getBody().get("sha");
            } catch (Exception e) {
                throw new CustomException(ErrorCode.GITHUB_FILE_FETCH_FAILED);
            }
        }

    public String getBranchHeadSha(Integer userId, String orgName, String repoName, String branch) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String token = githubTokenCrypto.decrypt(user.getGithubAccessToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");
        URI url = branchRefUri(orgName, repoName, "/git/ref/heads", branch);

        try {
            ResponseEntity<Map> response = new RestTemplate().exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map body = response.getBody();
            Map object = body == null ? null : (Map) body.get("object");
            String sha = object == null ? null : (String) object.get("sha");
            if (!StringUtils.hasText(sha)) {
                throw new CustomException(ErrorCode.GITHUB_BRANCH_FETCH_FAILED);
            }
            return sha;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.GITHUB_BRANCH_FETCH_FAILED);
        }
    }

    public String getCommitTreeSha(
            Integer userId, String orgName, String repoName, String commitSha) {
        HttpHeaders headers = githubHeaders(userId);
        String url = "https://api.github.com/repos/" + orgName + "/" + repoName
                + "/git/commits/" + commitSha;

        try {
            ResponseEntity<Map> response = new RestTemplate().exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map body = response.getBody();
            Map tree = body == null ? null : (Map) body.get("tree");
            String sha = tree == null ? null : (String) tree.get("sha");
            if (!StringUtils.hasText(sha)) {
                throw new CustomException(ErrorCode.GITHUB_COMMIT_FETCH_FAILED);
            }
            return sha;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.GITHUB_COMMIT_FETCH_FAILED);
        }
    }

    public String createBlob(Integer userId, String orgName, String repoName, String content) {
        HttpHeaders headers = githubHeaders(userId);
        Map<String, String> body = Map.of("content", content, "encoding", "utf-8");
        String url = "https://api.github.com/repos/" + orgName + "/" + repoName + "/git/blobs";

        try {
            ResponseEntity<Map> response = new RestTemplate().postForEntity(
                    url, new HttpEntity<>(body, headers), Map.class);
            String sha = response.getBody() == null ? null : (String) response.getBody().get("sha");
            if (!StringUtils.hasText(sha)) {
                throw new CustomException(ErrorCode.GITHUB_BLOB_CREATE_FAILED);
            }
            return sha;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            logGithubApiFailure("blob 생성", orgName, repoName, e);
            throw new CustomException(ErrorCode.GITHUB_BLOB_CREATE_FAILED);
        }
    }

    public Map<String, String> getFileModes(
            Integer userId,
            String orgName,
            String repoName,
            String treeSha,
            Set<String> paths
    ) {
        HttpHeaders headers = githubHeaders(userId);
        String url = "https://api.github.com/repos/" + orgName + "/" + repoName
                + "/git/trees/" + treeSha + "?recursive=1";

        try {
            ResponseEntity<Map> response = new RestTemplate().exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map body = response.getBody();
            if (body == null || Boolean.TRUE.equals(body.get("truncated"))) {
                throw new CustomException(ErrorCode.GITHUB_FILE_MODE_FETCH_FAILED);
            }

            Map<String, String> modes = new HashMap<>();
            List<Map<String, Object>> tree = (List<Map<String, Object>>) body.get("tree");
            if (tree != null) {
                for (Map<String, Object> item : tree) {
                    String path = (String) item.get("path");
                    if (paths.contains(path) && "blob".equals(item.get("type"))) {
                        modes.put(path, (String) item.get("mode"));
                    }
                }
            }
            if (modes.values().stream().anyMatch(mode -> !StringUtils.hasText(mode))) {
                throw new CustomException(ErrorCode.GITHUB_FILE_MODE_FETCH_FAILED);
            }
            // 트리에 없는 경로는 아직 저장소에 없는 새 파일이므로 일반 파일 모드(100644)로 채운다.
            for (String path : paths) {
                modes.putIfAbsent(path, "100644");
            }
            return modes;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            logGithubApiFailure("파일 모드 조회", orgName, repoName, e);
            throw new CustomException(ErrorCode.GITHUB_FILE_MODE_FETCH_FAILED);
        }
    }

    public String createTree(
            Integer userId,
            String orgName,
            String repoName,
            String baseTreeSha,
            List<GithubTreeItem> items
    ) {
        HttpHeaders headers = githubHeaders(userId);
        List<Map<String, String>> tree = items.stream()
                .map(item -> Map.of(
                        "path", item.path(),
                        "mode", item.mode(),
                        "type", "blob",
                        "sha", item.blobSha()))
                .toList();
        Map<String, Object> body = Map.of("base_tree", baseTreeSha, "tree", tree);
        String url = "https://api.github.com/repos/" + orgName + "/" + repoName + "/git/trees";

        try {
            ResponseEntity<Map> response = new RestTemplate().postForEntity(
                    url, new HttpEntity<>(body, headers), Map.class);
            String sha = response.getBody() == null ? null : (String) response.getBody().get("sha");
            if (!StringUtils.hasText(sha)) {
                throw new CustomException(ErrorCode.GITHUB_TREE_CREATE_FAILED);
            }
            return sha;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            logGithubApiFailure("tree 생성", orgName, repoName, e);
            throw new CustomException(ErrorCode.GITHUB_TREE_CREATE_FAILED);
        }
    }

    public String createCommit(
            Integer userId,
            String orgName,
            String repoName,
            String message,
            String treeSha,
            String parentCommitSha
    ) {
        HttpHeaders headers = githubHeaders(userId);
        Map<String, Object> body = Map.of(
                "message", message,
                "tree", treeSha,
                "parents", List.of(parentCommitSha));
        String url = "https://api.github.com/repos/" + orgName + "/" + repoName + "/git/commits";

        try {
            ResponseEntity<Map> response = new RestTemplate().postForEntity(
                    url, new HttpEntity<>(body, headers), Map.class);
            String sha = response.getBody() == null ? null : (String) response.getBody().get("sha");
            if (!StringUtils.hasText(sha)) {
                throw new CustomException(ErrorCode.GITHUB_COMMIT_CREATE_FAILED);
            }
            return sha;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            logGithubApiFailure("commit 생성", orgName, repoName, e);
            throw new CustomException(ErrorCode.GITHUB_COMMIT_CREATE_FAILED);
        }
    }

    private void logGithubApiFailure(
            String stage, String orgName, String repoName, Exception exception) {
        if (exception instanceof RestClientResponseException responseException) {
            log.warn("GitHub {} 실패 ({}/{} status={}): {}",
                    stage,
                    orgName,
                    repoName,
                    responseException.getStatusCode(),
                    responseException.getResponseBodyAsString());
            return;
        }
        log.error("GitHub {} 실패 ({}/{} type={}): {}",
                stage,
                orgName,
                repoName,
                exception.getClass().getSimpleName(),
                exception.getMessage());
    }

    public void updateBranchHead(
            Integer userId,
            String orgName,
            String repoName,
            String branch,
            String commitSha
    ) {
        HttpHeaders headers = githubHeaders(userId);
        Map<String, Object> body = Map.of("sha", commitSha, "force", false);
        URI url = branchRefUri(orgName, repoName, "/git/refs/heads", branch);

        try {
            RestTemplate restTemplate = new RestTemplate(new JdkClientHttpRequestFactory());
            restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (HttpClientErrorException e) {
            log.warn("GitHub 브랜치 갱신 거절 ({}/{}, branch={}, status={}): {}",
                    orgName, repoName, branch, e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(resolveBranchUpdateError(
                    e.getStatusCode().value(), e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("GitHub 브랜치 갱신 실패 ({}/{}, branch={}): {}",
                    orgName, repoName, branch, e.getMessage());
            throw new CustomException(ErrorCode.GITHUB_BRANCH_UPDATE_API_FAILED);
        }
    }

    static ErrorCode resolveBranchUpdateError(int status, String responseBody) {
        String message = responseBody == null ? "" : responseBody.toLowerCase(Locale.ROOT);
        if (status == 401 || status == 403 || message.contains("protected branch")) {
            return ErrorCode.GITHUB_BRANCH_UPDATE_FORBIDDEN;
        }
        if (status == 404) {
            return ErrorCode.GITHUB_BRANCH_NOT_FOUND;
        }
        if (status == 409 || message.contains("fast forward") || message.contains("fast-forward")) {
            return ErrorCode.GITHUB_BRANCH_UPDATE_FAILED;
        }
        if (status == 422) {
            return ErrorCode.GITHUB_BRANCH_UPDATE_REJECTED;
        }
        return ErrorCode.GITHUB_BRANCH_UPDATE_API_FAILED;
    }

    private HttpHeaders githubHeaders(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!StringUtils.hasText(user.getGithubAccessToken())) {
            throw new CustomException(ErrorCode.GITHUB_TOKEN_NOT_CONNECTED);
        }
        String token = githubTokenCrypto.decrypt(user.getGithubAccessToken());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");
        return headers;
    }

    private URI branchRefUri(String orgName, String repoName, String endpoint, String branch) {
        return UriComponentsBuilder
                .fromUriString("https://api.github.com/repos/" + orgName + "/" + repoName + endpoint)
                .path("/" + branch)
                .build()
                .encode()
                .toUri();
    }

    public String commitFile(Integer userId, String orgName, String repoName, String filePath, String branch,
                             String content, String sha, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String token = githubTokenCrypto.decrypt(user.getGithubAccessToken());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");

        String encodedContent = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        // sha가 없으면(새 파일 생성) GitHub Contents API 규격상 "sha" 필드 자체를 보내면 안 되므로 제외한다.
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("content", encodedContent);
        if (StringUtils.hasText(sha)) {
            body.put("sha", sha);
        }
        body.put("branch", branch);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = "https://api.github.com/repos/" + orgName + "/" + repoName + "/contents/" + filePath;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, Map.class);
            Map responseBody = response.getBody();
            Map commit = responseBody == null ? null : (Map) responseBody.get("commit");
            String commitSha = commit == null ? null : (String) commit.get("sha");
            if (!StringUtils.hasText(commitSha)) {
                throw new CustomException(ErrorCode.GITHUB_COMMIT_FAILED);
            }
            return commitSha;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.GITHUB_COMMIT_FAILED);
        }
    }
    
    public String getDefaultBranch(Integer userId, String orgName, String repoName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String token = githubTokenCrypto.decrypt(user.getGithubAccessToken());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://api.github.com/repos/" + orgName + "/" + repoName;

        try { 
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return (String) response.getBody().get("default_branch");
        } catch (Exception e) {
            throw new CustomException(ErrorCode.GITHUB_DEFAULT_BRANCH_FETCH_FAILED);
        }
    }    

    public GithubPullRequestResult createPullRequest(Integer userId, String orgName, String repoName,
                                                     String head, String base, String title, String body) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = githubHeaders(userId);

        Map<String, Object> requestBody = Map.of(
                "title", title,
                "head", head,
                "base", base,
                "body", body
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        String url = "https://api.github.com/repos/" + orgName + "/" + repoName + "/pulls";

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return toPullRequestResult(response.getBody());
        } catch (HttpClientErrorException e) {
            log.error("PR 생성 실패 ({}/{}, head={}, base={}): {}", orgName, repoName, head, base, e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY && e.getResponseBodyAsString().contains("A pull request already exists")) {
                return findOpenPullRequest(userId, orgName, repoName, head, base)
                        .orElseThrow(() -> new CustomException(ErrorCode.GITHUB_PR_ALREADY_OPEN));
            }
            throw new CustomException(ErrorCode.GITHUB_PR_CREATE_FAILED);
        } catch (Exception e) {
            log.error("PR 생성 중 알 수 없는 오류 ({}/{}, head={}, base={}): {}", orgName, repoName, head, base, e.getMessage());
            throw new CustomException(ErrorCode.GITHUB_PR_CREATE_FAILED);
        }
    }

    public Optional<GithubPullRequestResult> findOpenPullRequest(
            Integer userId,
            String orgName,
            String repoName,
            String head,
            String base
    ) {
        HttpHeaders headers = githubHeaders(userId);
        URI url = UriComponentsBuilder
                .fromUriString("https://api.github.com/repos/" + orgName + "/" + repoName + "/pulls")
                .queryParam("state", "open")
                .queryParam("head", orgName + ":" + head)
                .queryParam("base", base)
                .build()
                .encode()
                .toUri();

        try {
            ResponseEntity<List> response = new RestTemplate().exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), List.class);
            List results = response.getBody();
            if (results == null || results.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(toPullRequestResult((Map) results.get(0)));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.GITHUB_PR_CREATE_FAILED);
        }
    }

    public void closePullRequest(
            Integer userId, String orgName, String repoName, Integer pullRequestNumber) {
        HttpHeaders headers = githubHeaders(userId);
        String url = "https://api.github.com/repos/" + orgName + "/" + repoName
                + "/pulls/" + pullRequestNumber;
        try {
            new RestTemplate().exchange(
                    url,
                    HttpMethod.PATCH,
                    new HttpEntity<>(Map.of("state", "closed"), headers),
                    Map.class);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.GITHUB_PR_CLOSE_FAILED);
        }
    }

    private GithubPullRequestResult toPullRequestResult(Map responseBody) {
        if (responseBody == null) {
            throw new CustomException(ErrorCode.GITHUB_PR_CREATE_FAILED);
        }
        Map headInfo = (Map) responseBody.get("head");
        if (headInfo == null) {
            throw new CustomException(ErrorCode.GITHUB_PR_CREATE_FAILED);
        }
        return new GithubPullRequestResult(
                ((Number) responseBody.get("number")).intValue(),
                (String) responseBody.get("html_url"),
                ((String) responseBody.get("state")).toUpperCase(Locale.ROOT),
                Boolean.TRUE.equals(responseBody.get("draft")),
                (String) headInfo.get("sha"));
    }

    @Async
    public void fetchAndEmbedRepoFiles(Integer repoId, String orgName, String repoName, String branch, String token) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.set("Accept", "application/vnd.github+json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 1. 파일 트리 조회
            String treeUrl = "https://api.github.com/repos/" + orgName + "/" + repoName
                    + "/git/trees/" + branch + "?recursive=1";
            ResponseEntity<Map> treeResponse = restTemplate.exchange(treeUrl, HttpMethod.GET, entity, Map.class);
            Map<String, Object> treeBody = treeResponse.getBody();

            Boolean truncated = (Boolean) treeBody.get("truncated");
            if (Boolean.TRUE.equals(truncated)) {
                log.warn("{}/{} 레포의 {} 브랜치 파일 트리가 잘렸습니다(truncated). 일부 파일이 누락될 수 있습니다.", orgName, repoName, branch);
            }

            List<Map<String, Object>> treeItems = (List<Map<String, Object>>) treeBody.get("tree");
            if (treeItems == null) {
                log.info("{} 레포의 {} 브랜치에서 파일 트리를 가져오지 못했습니다.", repoName, branch);
                return;
            }

            // 2. 파일(blob)이면서 임베딩 대상 확장자인 것만 필터링 + 내용 조회
            List<IndexFileItem> fileList = new ArrayList<>();

            for (Map<String, Object> item : treeItems) {
                String path = (String) item.get("path");
                String type = (String) item.get("type");

                if (!"blob".equals(type) || EMBEDDABLE_EXTENSIONS.stream().noneMatch(path::endsWith)) {
                    continue;
                }

                try {
                    String contentUrl = "https://api.github.com/repos/" + orgName + "/" + repoName
                            + "/contents/" + path + "?ref=" + branch;
                    HttpHeaders rawHeaders = new HttpHeaders();
                    rawHeaders.setBearerAuth(token);
                    rawHeaders.set("Accept", "application/vnd.github.raw+json");
                    HttpEntity<String> rawEntity = new HttpEntity<>(rawHeaders);

                    ResponseEntity<String> fileResponse = restTemplate.exchange(contentUrl, HttpMethod.GET, rawEntity, String.class);
                    String content = fileResponse.getBody();

                    if (content == null || content.isBlank()) {
                        log.info("{} 파일 내용이 비어있어 임베딩 대상에서 제외합니다.", path);
                        continue;
                    }

                    fileList.add(new IndexFileItem(path, content));
                } catch (Exception e) {
                    log.warn("{} 파일 내용 조회 실패, 건너뜀: {}", path, e.getMessage());
                }
            }

            // 3. FastAPI로 임베딩 요청
            if (!fileList.isEmpty()) {
                embeddingService.requestEmbedding(repoId, fileList);
                log.info("{} 레포 임베딩 요청 완료 ({}개 파일)", repoName, fileList.size());
            } else {
                log.info("{} 레포에 임베딩 대상 파일이 없습니다.", repoName);
            }

        } catch (Exception e) {
            log.error("{} 레포 임베딩 처리 중 오류: {}", repoName, e.getMessage());
        }
    }

    @Async
    @Transactional(readOnly = false)
    public void processPushEmbedding(String orgName, String repoName, String branch,
                                    Set<String> addedPaths, Set<String> modifiedPaths, Set<String> removedPaths) {

        GithubRepo repo = githubRepoRepository.findByNameAndOrganization(repoName, orgName).orElse(null);
        if (repo == null) {
            log.warn("{}/{} 레포를 찾을 수 없어 재임베딩을 건너뜁니다.", orgName, repoName);
            return;
        }

        // 이 레포에 연동된 아무 사용자의 토큰이나 사용
        User anyUser = userRepoRepository.findAllByGithubRepo_Id(repo.getId()).stream()
                .findFirst().map(UserRepo::getUser).orElse(null);
        if (anyUser == null) {
            log.warn("{} 레포에 연동된 사용자가 없어 재임베딩을 건너뜁니다.", repoName);
            return;
        }
        String token = githubTokenCrypto.decrypt(anyUser.getGithubAccessToken());

        for (String path : removedPaths) {
            embeddingService.removeFileEmbeddings(repo.getId(), path);
        }

        for (String path : modifiedPaths) {
            embeddingService.removeFileEmbeddings(repo.getId(), path);
        }

        Set<String> pathsToEmbed = new HashSet<>();
        pathsToEmbed.addAll(addedPaths);
        pathsToEmbed.addAll(modifiedPaths);

        List<IndexFileItem> fileList = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();

        for (String path : pathsToEmbed) {
            if (EMBEDDABLE_EXTENSIONS.stream().noneMatch(path::endsWith)) continue;

            try {
                String contentUrl = "https://api.github.com/repos/" + orgName + "/" + repoName
                        + "/contents/" + path + "?ref=" + branch;
                HttpHeaders rawHeaders = new HttpHeaders();
                rawHeaders.setBearerAuth(token);
                rawHeaders.set("Accept", "application/vnd.github.raw+json");
                HttpEntity<String> rawEntity = new HttpEntity<>(rawHeaders);

                ResponseEntity<String> fileResponse = restTemplate.exchange(contentUrl, HttpMethod.GET, rawEntity, String.class);
                String content = fileResponse.getBody();

                if (content == null || content.isBlank()) continue;

                fileList.add(new IndexFileItem(path, content));
            } catch (Exception e) {
                log.warn("{} 파일 내용 조회 실패, 건너뜀: {}", path, e.getMessage());
            }
        }

        if (!fileList.isEmpty()) {
            embeddingService.requestEmbedding(repo.getId(), fileList);
            log.info("{} 레포 push 재임베딩 완료 (added+modified {}개, removed {}개)",
                    repoName, fileList.size(), removedPaths.size());
        }
    }
}
