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


import java.time.LocalDate;
import java.util.Map;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GithubService {

    private final UserRepository userRepository;
    private final GithubTokenCrypto githubTokenCrypto;
    private final GithubRepoRepository githubRepoRepository;
    private final UserRepoRepository userRepoRepository;

    public GithubService(UserRepository userRepository, GithubTokenCrypto githubTokenCrypto, GithubRepoRepository githubRepoRepository, UserRepoRepository userRepoRepository) {
        this.userRepository = userRepository;
        this.githubTokenCrypto = githubTokenCrypto;
        this.githubRepoRepository = githubRepoRepository;
        this.userRepoRepository = userRepoRepository;
    }

    @Transactional
    public Object connectAndFetchRepos(Integer userId, String orgName, String githubToken) {
        User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String tokenToUse;
        
        if (githubToken != null && !githubToken.isBlank()) {
        // 새 토큰이 넘어왔으면 그걸 사용 (재연동/갱신 케이스)
            tokenToUse = githubToken;
        } 
        else if (user.getGithubAccessToken() != null) {
            // 없으면 기존 저장된 토큰을 복호화해서 재사용
            tokenToUse = githubTokenCrypto.decrypt(user.getGithubAccessToken());
        } 
        else {
            throw new IllegalArgumentException("연동된 GitHub 토큰이 없습니다. 토큰을 입력해주세요.");
        }

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
                                return githubRepoRepository.save(newRepo);
                            });
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

            if (githubToken != null && !githubToken.isBlank()) {
                user.updateGithubToken(githubTokenCrypto.encrypt(tokenToUse));
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

    /** 로그인한 사용자에게 연결된 Repository 상세정보만 반환한다. */
    public RepoResponse getUserRepo(Integer userId, Integer repoId) {
        UserRepo userRepo = userRepoRepository
                .findByUser_IdAndGithubRepo_Id(userId, repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));

        return RepoResponse.from(userRepo.getGithubRepo());
    }
}
