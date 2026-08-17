package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.user.GithubConnect;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.aivle.bigproject.service.GithubService;
import com.aivle.bigproject.dto.repo.GithubFileContent;
import com.aivle.bigproject.dto.repo.RepoResponse;
import com.aivle.bigproject.dto.repo.BranchResponse;
import com.aivle.bigproject.dto.repo.GithubPullRequestResponse;
import com.aivle.bigproject.dto.repo.RepoTreeResponse;
import java.util.List;


/**
 * GitHub 저장소 관련 API를 제공하는 REST Controller.
 *
 * GitHub 저장소의 연결, 조회 및 관리 기능을 제공
 */
@RestController
@RequestMapping("/api/repos")
public class GithubController {

    private final GithubService githubService;

    /**
     * 생성자 주입을 통해 GithubService를 초기화
     * @param githubService
     */
    public GithubController(GithubService githubService) {
        this.githubService = githubService;
    }

     /**
     * 로그인한 사용자의 GitHub 조직과 저장소를 연동한다.
     *
     * 요청받은 GitHub 조직명을 기반으로 사용자가 접근 가능한 저장소 정보를
     * 조회하고 서비스 내부 저장소 정보와 연동
     */
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

    /**
     * 로그인한 사용자가 연동한 GitHub 저장소 목록을 조회
     *
     * JWT에서 사용자 ID를 추출하여 해당 사용자와 연결된
     * 모든 GitHub 저장소 정보를 반환한다.
     */
    @GetMapping
    public ResponseEntity<List<RepoResponse>> getUserRepos(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Integer userId = Integer.valueOf(jwt.getSubject());
        
        List<RepoResponse> repos = githubService.getUserRepos(userId);

        return ResponseEntity.ok(repos);
    }

     /**
     * 로그인한 사용자가 연동한 특정 GitHub 저장소의 정보 조회
     *
     * 사용자 ID와 저장소 ID를 기반으로 해당 저장소의 상세 정보 반환.
     */
    @GetMapping("/{repoId}")
    public ResponseEntity<RepoResponse> getUserRepo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId
    ) {
        Integer userId = Integer.valueOf(jwt.getSubject());
        return ResponseEntity.ok(githubService.getUserRepo(userId, repoId));
    }

    /**
     * 특정 GitHub 저장소의 브랜치 목록을 조회
     *
     * 로그인한 사용자가 접근할 수 있는 저장소인지 확인한 후
     * 해당 저장소의 브랜치 정보를 반환
     */
    @GetMapping("/{repoId}/branches")
    public ResponseEntity<List<BranchResponse>> getRepositoryBranches(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId
    ) {
        return ResponseEntity.ok(githubService.getRepositoryBranches(
                Integer.valueOf(jwt.getSubject()), repoId));
    }

    /**
     * 특정 GitHub 저장소의 파일 및 디렉터리 트리를 조회
     *
     * 지정한 브랜치를 기준으로 저장소의 파일 구조를 조회하며,
     * issuesOnly 값에 따라 분석 이슈가 존재하는 파일만 조회
     */
    @GetMapping("/{repoId}/tree")
    public ResponseEntity<RepoTreeResponse> getRepositoryTree(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId,
            @RequestParam String branch,
            @RequestParam(defaultValue = "false") boolean issuesOnly
    ) {
        return ResponseEntity.ok(githubService.getRepositoryTree(
                Integer.valueOf(jwt.getSubject()), repoId, branch, issuesOnly));
    }

    /**
     * 특정 GitHub 저장소의 파일 내용을 조회한다.
     *
     * 저장소 ID, 브랜치명 및 파일 경로를 기반으로
     * GitHub에서 해당 파일의 최신 내용을 조회한다\
     */
    @GetMapping("/{repoId}/content")
    public ResponseEntity<GithubFileContent> getRepositoryFileContent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId,
            @RequestParam String path,
            @RequestParam String branch
    ) {
        return ResponseEntity.ok(githubService.getLatestFileContent(
                Integer.valueOf(jwt.getSubject()), repoId, path, branch));
    }


    /**
     * 특정 GitHub 저장소의 Pull Request 목록을 조회한다.
     *
     * 조회 범위(scope), Pull Request 상태(state), 페이지 번호 및 크기를
     * 기준으로 저장소의 Pull Request 목록을 조회
     */
    @GetMapping("/{repoId}/pull-requests")
    public ResponseEntity<List<GithubPullRequestResponse>> getRepositoryPullRequests(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId,
            @RequestParam(defaultValue = "mine") String scope,
            @RequestParam(defaultValue = "all") String state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(githubService.getRepositoryPullRequests(
                Integer.valueOf(jwt.getSubject()), repoId, scope, state, page, size));
    }

    /**
     * 로그인한 사용자가 연결한 GitHub 조직에 Webhook 등록을 요청한다.
     */
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
