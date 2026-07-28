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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collections;

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

    private static final Set<String> EMBEDDABLE_EXTENSIONS = Set.of(
            ".java", ".py", ".js", ".jsx", ".ts", ".tsx"
    );

    public GithubService(UserRepository userRepository, GithubTokenCrypto githubTokenCrypto, GithubRepoRepository githubRepoRepository, EmbeddingService embeddingService, UserRepoRepository userRepoRepository, RepoEmbeddingRepository repoEmbeddingRepository, @Value("${github.webhook-callback-url}") String webhookCallbackUrl,
        @Value("${github.webhook-secret}") String webhookSecret) {
        this.userRepository = userRepository;
        this.githubTokenCrypto = githubTokenCrypto;
        this.githubRepoRepository = githubRepoRepository;
        this.userRepoRepository = userRepoRepository;
        this.embeddingService = embeddingService;
        this.repoEmbeddingRepository = repoEmbeddingRepository;
        this.webhookCallbackUrl = webhookCallbackUrl;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public Object connectAndFetchRepos(Integer userId, String orgName) {
        User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.getGithubAccessToken() == null) {
            throw new IllegalArgumentException("연동된 GitHub 토큰이 없습니다. OAuth 인증을 먼저 진행해 주세요.");
        }

        String tokenToUse = githubTokenCrypto.decrypt(user.getGithubAccessToken());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenToUse); 
        headers.set("Accept", "application/vnd.github+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = "https://api.github.com/orgs/" + orgName + "/repos?per_page=100&sort=updated";

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> repoList = (List<Map<String, Object>>) response.getBody();

            if (repoList != null) {
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
                                registerWebhook(saved, tokenToUse);
                                fetchAndEmbedRepoFiles(saved.getId(), organization, repoName, "dev", tokenToUse);
                                return saved;
                            });
                    if (currentRepo.getWebhookId() == null) {
                        registerWebhook(currentRepo, tokenToUse);
                    }

                    if (!repoEmbeddingRepository.existsByGithubRepo_Id(currentRepo.getId())) {
                        fetchAndEmbedRepoFiles(currentRepo.getId(), organization, repoName, "dev", tokenToUse);
                    }

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
            throw new IllegalArgumentException("GitHub 연동에 실패했습니다. 올바른 조직명과 권한이 있는 토큰인지 확인해주세요.");
        }
    }

    public Object getRepositoryTree(Integer userId, String orgName, String repoName, String branch) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String encryptedToken = user.getGithubAccessToken();
        if (encryptedToken == null) {
            throw new IllegalArgumentException("연동된 GitHub 토큰이 없습니다.");
        }

        String decryptedToken = githubTokenCrypto.decrypt(encryptedToken);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(decryptedToken);
        headers.set("Accept", "application/vnd.github+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = "https://api.github.com/repos/" + orgName + "/" + repoName + "/git/trees/" + branch + "?recursive=1";

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class); 
            Map<String, Object> body = response.getBody();

            Boolean truncated = (Boolean) body.get("truncated");
            if (Boolean.TRUE.equals(truncated)) {
                log.warn("{}/{} 레포의 {} 브랜치 파일 트리가 잘렸습니다(truncated). 일부 파일이 누락될 수 있습니다.", orgName, repoName, branch);
            }

            return body;
        } catch (Exception e) {
            throw new IllegalArgumentException("트리 정보를 가져오는데 실패했습니다.");
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
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubTokenCrypto.decrypt(user.getGithubAccessToken()));
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2026-03-10");
        String url = "https://api.github.com/repos/" + repo.getOrganization()
                + "/" + repo.getName() + "/branches?per_page=100";

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
                        return new BranchResponse(
                                (String) branch.get("name"),
                                commit == null ? null : (String) commit.get("sha"),
                                Boolean.TRUE.equals(branch.get("protected"))
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
                "events", List.of("push"),
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

    public void commitFile(Integer userId, String orgName, String repoName, String filePath, String branch,
                            String content, String sha, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String token = githubTokenCrypto.decrypt(user.getGithubAccessToken());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");

        String encodedContent = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> body = Map.of(
            "message", message,
            "content", encodedContent,
            "sha", sha,
            "branch", branch );
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = "https://api.github.com/repos/" + orgName + "/" + repoName + "/contents/" + filePath;

        try { restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
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

    public String createPullRequest(Integer userId, String orgName, String repoName, 
                                    String head, String base, String title, String body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String token = githubTokenCrypto.decrypt(user.getGithubAccessToken());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");

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
            return (String) response.getBody().get("html_url");
        } catch (Exception e) {
            throw new CustomException(ErrorCode.GITHUB_PR_CREATE_FAILED);
        }
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
}
