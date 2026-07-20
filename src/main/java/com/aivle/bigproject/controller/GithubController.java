package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.user.GithubConnect; 
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> connectRepository(@Valid @RequestBody GithubConnect request) {
        try {
            String token = request.githubToken(); 
            
            System.out.println("프론트에서 넘어온 깃허브 토큰: " + token);
            
            return ResponseEntity.ok().body("레포지토리 연동이 성공적으로 완료되었습니다.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    } 
}