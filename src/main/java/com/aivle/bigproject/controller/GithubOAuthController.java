package com.aivle.bigproject.controller;

import com.aivle.bigproject.service.GithubOAuthService;

import com.aivle.bigproject.service.GithubService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
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

    @Value("${app.frontend-url}")
    private String frontendUrl;

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
                    log.error("GitHub 콜백: 토큰 저장은 성공했으나 조직 repo 연동 실패 (orgName={}): {}", orgName, e.getMessage(), e);
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(frontendRedirect("partial"))
                            .build();
                }
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(frontendRedirect("success"))
                    .build();

        } catch (Exception e) {
            log.error("GitHub OAuth 콜백 처리 실패 (state={}): {}", state, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(frontendRedirect("error"))
                    .build();
        }
    }

    /**
     * GitHub OAuth 처리 결과를 전달하기 위한 Frontend URI를 생성한다.
     */
    private URI frontendRedirect(String status) {
        return URI.create(frontendUrl.replaceAll("/+$", "")
                + "/?page=mypage&github=" + status + "#github-section");
    }
}
