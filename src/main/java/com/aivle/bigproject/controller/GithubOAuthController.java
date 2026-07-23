package com.aivle.bigproject.controller;

import com.aivle.bigproject.service.GithubOAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/github/oauth")
public class GithubOAuthController {

    private final GithubOAuthService githubOAuthService;

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.oauth-redirect-uri}")
    private String redirectUri;

    @Value("${github.oauth-scope}")
    private String scope;

    public GithubOAuthController(GithubOAuthService githubOAuthService) {
        this.githubOAuthService = githubOAuthService;
    }

    /**
     * 1. 프론트엔드가 [GitHub 연동하기] 버튼을 누르면 호출
     *    GitHub 인증 페이지로 이동할 URL을 반환합니다.
     */
    @GetMapping("/authorize-url")
    public ResponseEntity<Map<String, String>> getAuthorizeUrl(@AuthenticationPrincipal Jwt jwt) {
        Integer userId = Integer.valueOf(jwt.getSubject());
        
        // CSRF 방지 및 콜백에서 어떤 유저의 요청인지 식별하기 위해 state 파라미터에 userId 사용
        String state = String.valueOf(userId);
        
        String url = String.format(
                "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                clientId,
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(scope, StandardCharsets.UTF_8),
                state
        );

        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * 2. 사용자가 GitHub 동의 화면에서 [승인]을 눌렀을 때 GitHub이 리다이렉트해오는 콜백 주소
     */
    @GetMapping("/callback")
    public ResponseEntity<String> githubCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        try {
            Integer userId = Integer.valueOf(state);
            
            // GitHub code -> access_token 교환 및 DB 저장
            githubOAuthService.exchangeCodeAndSaveToken(userId, code);

            // 완료 후 프론트엔드 연동 완료 페이지/대시보드로 리다이렉트 시키거나 메시지 반환
            return ResponseEntity.ok("GitHub 계정이 성공적으로 연동되었습니다! 창을 닫고 서비스로 돌아가세요.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("GitHub 연동 중 오류 발생: " + e.getMessage());
        }
    }
}