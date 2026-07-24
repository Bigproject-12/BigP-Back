package com.aivle.bigproject.service;

import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.UserRepository;
import com.aivle.bigproject.security.GithubTokenCrypto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class GithubOAuthService {

    private final UserRepository userRepository;
    private final GithubTokenCrypto githubTokenCrypto;

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.client-secret}")
    private String clientSecret;

    @Value("${github.oauth-redirect-uri}")
    private String redirectUri;

    public GithubOAuthService(UserRepository userRepository, GithubTokenCrypto githubTokenCrypto) {
        this.userRepository = userRepository;
        this.githubTokenCrypto = githubTokenCrypto;
    }

    @Transactional
    public void exchangeCodeAndSaveToken(Integer userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("code", code);
        requestBody.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://github.com/login/oauth/access_token",
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("access_token")) {
            throw new IllegalArgumentException("GitHub 토큰 발급에 실패했습니다: " + body);
        }

        String accessToken = (String) body.get("access_token");
        /***************************************************************** */
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        userHeaders.set("Accept", "application/vnd.github+json");
        HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                userRequest,
                Map.class
        );
        Map<String, Object> githubUser = userResponse.getBody();
        String realGitId = String.valueOf(githubUser.get("id"));       
        String realGitName = (String) githubUser.get("login");        

        userRepository.findByGitId(realGitId).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(userId)) {
                throw new IllegalStateException("이미 다른 계정에 연동된 GitHub 계정입니다.");
            }
        });

        user.setGitId(realGitId);
        user.setGitName(realGitName);

        String encryptedToken = githubTokenCrypto.encrypt(accessToken);
        user.updateGithubToken(encryptedToken);
    }
}