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
//GitHub OAuth 인증 및 사용자 계정 연동을 처리하는 Service
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
    //GitHub OAuth 인증 코드를 Access Token으로 교환하고 사용자 계정에 연동
    @Transactional
    public void exchangeCodeAndSaveToken(Integer userId, String code) {
        // 로그인 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RestTemplate restTemplate = new RestTemplate();
        // GitHub Access Token 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        // OAuth 인증 코드 교환 요청 데이터 생성
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("code", code);
        requestBody.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(requestBody, headers);
        // GitHub OAuth 서버에 Access Token 발급 요청
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://github.com/login/oauth/access_token",
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        // Access Token 발급 여부 확인
        if (body == null || !body.containsKey("access_token")) {
            throw new IllegalArgumentException("GitHub 토큰 발급에 실패했습니다: " + body);
        }

        String accessToken = (String) body.get("access_token");
        // 발급받은 Access Token으로 GitHub 사용자 정보 요청
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
        // GitHub 사용자 정보 조회
        Map<String, Object> githubUser = userResponse.getBody();
        String realGitId = String.valueOf(githubUser.get("id"));       
        String realGitName = (String) githubUser.get("login");        
        // 동일한 GitHub 계정이 다른 사용자에게 연동되어 있는지 확인(1계정 중복 연동 방지)
        userRepository.findByGitId(realGitId).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(userId)) {
                throw new IllegalStateException("이미 다른 계정에 연동된 GitHub 계정입니다.");
            }
        });
        // GitHub 계정 정보 저장
        user.setGitId(realGitId);
        user.setGitName(realGitName);
        // Access Token 암호화
        String encryptedToken = githubTokenCrypto.encrypt(accessToken);
        // 암호화된 GitHub Access Token 저장
        user.updateGithubToken(encryptedToken);
    }
}