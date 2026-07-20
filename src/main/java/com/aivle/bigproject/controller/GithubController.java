package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.user.GithubConnect;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.aivle.bigproject.service.UserService;

@RestController
@RequestMapping("/api/repos")
public class GithubController {

    private final UserService userService;

    public GithubController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<String> connectRepository(
            @Valid @RequestBody GithubConnect request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        userService.saveGithubToken(Integer.valueOf(jwt.getSubject()), request.githubToken());
        return ResponseEntity.ok("레포지토리 연동이 성공적으로 완료되었습니다.");
    }
}
