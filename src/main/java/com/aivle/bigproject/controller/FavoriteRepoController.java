package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.repo.RepoResponse;
import com.aivle.bigproject.service.FavoriteRepoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;


/**
 * 즐겨찾기 저장소(Favorite Repository) 관련 API를 제공하는 REST Controller.
 *
 * 로그인한 사용자의 저장소 즐겨찾기 등록, 삭제 및 목록 조회 기능을 제공한다.
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteRepoController {
    private final FavoriteRepoService favoriteRepoService;

    public FavoriteRepoController(FavoriteRepoService favoriteRepoService) {
        this.favoriteRepoService = favoriteRepoService;
    }

    /**
     * 로그인한 사용자의 즐겨찾기에 특정 저장소를 추가한다.
     *
     * JWT에서 사용자 ID를 추출하고, 전달받은 저장소 ID를
     * 해당 사용자의 즐겨찾기 목록에 등록한다.
     */
    @PostMapping("/{repoId}")
    public ResponseEntity<Void> addFavorite(@AuthenticationPrincipal Jwt jwt, 
    @PathVariable Integer repoId) {
        favoriteRepoService.addFavorite(Integer.valueOf(jwt.getSubject()), repoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 로그인한 사용자의 즐겨찾기에서 특정 저장소를 삭제한다.
     *
     * JWT에서 사용자 ID를 추출하고, 전달받은 저장소 ID에 해당하는
     * 저장소를 사용자의 즐겨찾기 목록에서 제거한다.
     */
    @DeleteMapping("/{repoId}")
    public ResponseEntity<Void> removeFavorite(@AuthenticationPrincipal Jwt jwt,
    @PathVariable Integer repoId) {
        favoriteRepoService.removeFavorite(Integer.valueOf(jwt.getSubject()), repoId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 로그인한 사용자의 즐겨찾기 저장소 목록을 조회한다.
     *
     * JWT에서 사용자 ID를 추출하고, 해당 사용자의 즐겨찾기 저장소 목록을 반환한다.
     */
    @GetMapping
    public ResponseEntity<List<RepoResponse>> getFavoriteRepos(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(favoriteRepoService.getFavoriteRepos(Integer.valueOf(jwt.getSubject())));
    }
}
