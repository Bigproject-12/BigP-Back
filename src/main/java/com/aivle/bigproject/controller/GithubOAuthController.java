package com.aivle.bigproject.controller;

import com.aivle.bigproject.service.GithubOAuthService;

import com.aivle.bigproject.service.GithubService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/github/oauth")
public class GithubOAuthController {

    private final GithubOAuthService githubOAuthService;
    private final GithubService githubService;

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.oauth-redirect-uri}")
    private String redirectUri;

    @Value("${github.oauth-scope}")
    private String scope;

    public GithubOAuthController(GithubOAuthService githubOAuthService, GithubService githubService) {
        this.githubOAuthService = githubOAuthService;
        this.githubService = githubService;
    }

    /**
     * 1. 프론트엔드가 [GitHub 연동하기] 버튼을 누르면 호출
     *    GitHub 인증 페이지로 이동할 URL을 반환합니다.
     */
    @GetMapping("/authorize-url")
    public ResponseEntity<Map<String, String>> getAuthorizeUrl(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String orgName) {
        Integer userId = Integer.valueOf(jwt.getSubject());

        String orgPart = (orgName != null && !orgName.isBlank()) ? orgName.trim() : "";
        String state = userId + "|" + orgPart;

        String url = String.format(
                "https://github.com/login/oauth/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                clientId,
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(scope, StandardCharsets.UTF_8),
                URLEncoder.encode(state, StandardCharsets.UTF_8)
        );

        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * 2. 사용자가 GitHub 동의 화면에서 [승인]을 눌렀을 때 GitHub이 리다이렉트해오는 콜백 주소
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> githubCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        try {
            String[] parts = state.split("\\|", 2);
            Integer userId = Integer.valueOf(parts[0]);
            String orgName = parts.length > 1 ? parts[1] : "";

            githubOAuthService.exchangeCodeAndSaveToken(userId, code);

            if (!orgName.isBlank()) {
                try {
                    githubService.connectAndFetchRepos(userId, orgName);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create("http://localhost:5173/?page=mypage&github=partial#github-section"))
                            .build();
                }
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:5173/?page=mypage&github=success#github-section"))
                    .build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:5173/?page=mypage&github=error#github-section"))
                    .build();
        }
    }
}