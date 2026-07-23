package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.repo.RepoResponse;
import com.aivle.bigproject.service.FavoriteRepoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteRepoController {
    private final FavoriteRepoService favoriteRepoService;

    public FavoriteRepoController(FavoriteRepoService favoriteRepoService) {
        this.favoriteRepoService = favoriteRepoService;
    }

    @PostMapping("/{repoId}")
    public ResponseEntity<Void> addFavorite(@AuthenticationPrincipal Jwt jwt, 
    @PathVariable Integer repoId) {
        favoriteRepoService.addFavorite(Integer.valueOf(jwt.getSubject()), repoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{repoId}")
    public ResponseEntity<Void> removeFavorite(@AuthenticationPrincipal Jwt jwt,
    @PathVariable Integer repoId) {
        favoriteRepoService.removeFavorite(Integer.valueOf(jwt.getSubject()), repoId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    public ResponseEntity<List<RepoResponse>> getFavoriteRepos(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(favoriteRepoService.getFavoriteRepos(Integer.valueOf(jwt.getSubject())));
    }
}
