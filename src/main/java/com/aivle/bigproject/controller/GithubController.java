package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.user.GithubConnect;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.aivle.bigproject.service.GithubService;

import com.aivle.bigproject.dto.repo.RepoResponse; 
import com.aivle.bigproject.dto.repo.BranchResponse;
import java.util.List;

@RestController
@RequestMapping("/api/repos")
public class GithubController {

    private final GithubService githubService;

    public GithubController(GithubService githubService) {
        this.githubService = githubService;
    }

    @PostMapping
    public ResponseEntity<?> connectRepository(
            @Valid @RequestBody GithubConnect request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try{
            System.out.println("입력된 올거나이즈 이름: " + request.orgName());
            Integer userId = Integer.valueOf(jwt.getSubject());
            
            Object repos = githubService.connectAndFetchRepos(
                    userId, 
                    request.orgName()
            );

            return ResponseEntity.ok(repos);
        }
        catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("서버 내부 오류: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<RepoResponse>> getUserRepos(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Integer userId = Integer.valueOf(jwt.getSubject());
        
        List<RepoResponse> repos = githubService.getUserRepos(userId);

        return ResponseEntity.ok(repos);
    }

    @GetMapping("/{repoId}")
    public ResponseEntity<RepoResponse> getUserRepo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId
    ) {
        Integer userId = Integer.valueOf(jwt.getSubject());
        return ResponseEntity.ok(githubService.getUserRepo(userId, repoId));
    }

    @GetMapping("/{repoId}/branches")
    public ResponseEntity<List<BranchResponse>> getRepositoryBranches(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId
    ) {
        return ResponseEntity.ok(githubService.getRepositoryBranches(
                Integer.valueOf(jwt.getSubject()), repoId));
    }

    @PostMapping("/org-webhook")
    public ResponseEntity<String> registerOrgWebhook(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String orgName
    ) {
        Integer userId = Integer.valueOf(jwt.getSubject());
        // 저장된 토큰을 복호화해서 사용 (GithubService에 이 로직을 노출하는 별도 메서드가 필요할 수도 있음)
        githubService.triggerOrgWebhookRegistration(userId, orgName);
        return ResponseEntity.ok("조직 Webhook 등록 시도 완료 (로그 확인 필요)");
    }
}
