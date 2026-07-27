package com.aivle.bigproject.service;

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


import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;


import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

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

    public GithubService(UserRepository userRepository, GithubTokenCrypto githubTokenCrypto, GithubRepoRepository githubRepoRepository, UserRepoRepository userRepoRepository, @Value("${github.webhook-callback-url}") String webhookCallbackUrl,
        @Value("${github.webhook-secret}") String webhookSecret) {
        this.userRepository = userRepository;
        this.githubTokenCrypto = githubTokenCrypto;
        this.githubRepoRepository = githubRepoRepository;
        this.userRepoRepository = userRepoRepository;
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
                                return saved;
                            });
                    if (currentRepo.getWebhookId() == null) {
                        registerWebhook(currentRepo, tokenToUse);
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
            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, entity, Object.class);
            return response.getBody();
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

}