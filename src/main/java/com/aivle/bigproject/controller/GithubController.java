package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.user.GithubConnect;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.aivle.bigproject.service.GithubService;

import com.aivle.bigproject.dto.repo.RepoResponse; 
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
                    request.orgName(), 
                    request.githubToken()
            );

            return ResponseEntity.ok(repos);
        }
        catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 내부 오류가 발생했습니다.");
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
}
