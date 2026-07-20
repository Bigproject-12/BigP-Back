package com.aivle.bigproject.service;

import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.UserRepository;
import com.aivle.bigproject.security.GithubTokenCrypto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GithubService {

    private final UserRepository userRepository;
    private final GithubTokenCrypto githubTokenCrypto;

    public GithubService(UserRepository userRepository, GithubTokenCrypto githubTokenCrypto) {
        this.userRepository = userRepository;
        this.githubTokenCrypto = githubTokenCrypto;
    }

    @Transactional
    public Object connectAndFetchRepos(Integer userId, String orgName, String githubToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken); 
        headers.set("Accept", "application/vnd.github+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = "https://api.github.com/orgs/" + orgName + "/repos?per_page=100&sort=updated";

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            user.updateGithubToken(githubTokenCrypto.encrypt(githubToken));

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
}