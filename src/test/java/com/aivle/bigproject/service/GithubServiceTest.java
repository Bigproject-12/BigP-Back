package com.aivle.bigproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.UserRepo;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.ai.service.EmbeddingService;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.GithubRepoRepository;
import com.aivle.bigproject.repository.GithubPullRequestRepository;
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.RepoEmbeddingRepository;
import com.aivle.bigproject.repository.UserRepoRepository;
import com.aivle.bigproject.repository.UserRepository;
import com.aivle.bigproject.security.GithubTokenCrypto;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
@ExtendWith(MockitoExtension.class)
class GithubServiceTest {

    @Mock UserRepository userRepository;
    @Mock GithubTokenCrypto githubTokenCrypto;
    @Mock GithubRepoRepository githubRepoRepository;
    @Mock EmbeddingService embeddingService;
    @Mock UserRepoRepository userRepoRepository;
    @Mock RepoEmbeddingRepository repoEmbeddingRepository;
    @Mock GithubPullRequestRepository githubPullRequestRepository;
    @Mock AnalysisRepository analysisRepository;
    @Mock RestTemplate restTemplate;

    @Test
    void getUserRepoReturnsOnlyConnectedRepository() {
        GithubRepo repo = GithubRepo.builder().id(10).name("BigP").build();
        UserRepo userRepo = UserRepo.builder().githubRepo(repo).build();
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 10))
                .thenReturn(Optional.of(userRepo));
        GithubService service = service();

        var response = service.getUserRepo(1, 10);

        assertEquals(10, response.id());
        assertEquals("BigP", response.name());
    }

    @Test
    void getUserRepoRejectsUnconnectedRepository() {
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 99))
                .thenReturn(Optional.empty());
        GithubService service = service();

        assertThrows(CustomException.class, () -> service.getUserRepo(1, 99));
    }

    @Test
    void getBranchesRejectsUnconnectedRepository() {
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 99))
                .thenReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service().getRepositoryBranches(1, 99));

        assertEquals(ErrorCode.REPO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getBranchesRequiresGithubToken() {
        User user = User.builder().id(1).build();
        GithubRepo repo = GithubRepo.builder().id(10).build();
        UserRepo userRepo = UserRepo.builder().user(user).githubRepo(repo).build();
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 10))
                .thenReturn(Optional.of(userRepo));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service().getRepositoryBranches(1, 10));

        assertEquals(ErrorCode.GITHUB_TOKEN_NOT_CONNECTED, exception.getErrorCode());
    }

    @Test
    void getRepositoryTreeReturnsConnectedRepositoryFiles() {
        User user = User.builder()
                .id(1)
                .githubAccessToken("encrypted-token")
                .build();
        GithubRepo repo = GithubRepo.builder()
                .id(10)
                .organization("aivle")
                .name("BigP-Back")
                .build();
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 10))
                .thenReturn(Optional.of(UserRepo.builder().user(user).githubRepo(repo).build()));
        when(githubTokenCrypto.decrypt("encrypted-token")).thenReturn("github-token");
        AnalysisRepository.FileIssueSummary fileIssues =
                org.mockito.Mockito.mock(AnalysisRepository.FileIssueSummary.class);
        when(fileIssues.getFilePath()).thenReturn("src/App.java");
        when(fileIssues.getTotalIssueCount()).thenReturn(6);
        when(fileIssues.getSecurityIssueCount()).thenReturn(2);
        when(fileIssues.getInefficiencyIssueCount()).thenReturn(1);
        when(analysisRepository.findLatestFileIssues(1, 10, "feature/test"))
                .thenReturn(List.of(fileIssues));
        Map<String, Object> treeBody = Map.of(
                "truncated", false,
                "tree", List.of(
                        Map.of("path", "src", "type", "tree", "sha", "tree-sha"),
                        Map.of(
                                "path", "src/App.java",
                                "type", "blob",
                                "sha", "blob-sha",
                                "size", 120L))
        );
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                org.mockito.ArgumentMatchers
                        .<ParameterizedTypeReference<Map<String, Object>>>any()))
                .thenReturn(ResponseEntity.ok(treeBody));

        var response = service().getRepositoryTree(1, 10, "feature/test");

        assertEquals(10, response.repoId());
        assertEquals("feature/test", response.branch());
        assertEquals(2, response.items().size());
        assertEquals("src/App.java", response.items().get(1).path());
        assertEquals(120L, response.items().get(1).size());
        assertEquals(6, response.items().get(0).totalIssueCount());
        assertEquals(6, response.items().get(1).totalIssueCount());
        assertEquals(2, response.items().get(1).securityIssueCount());
        assertEquals(1, response.items().get(1).inefficiencyIssueCount());
        assertEquals(3, response.items().get(1).otherIssueCount());
    }

    @Test
    void getRepositoryTreeRejectsUnconnectedRepository() {
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 99))
                .thenReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service().getRepositoryTree(1, 99, "dev"));

        assertEquals(ErrorCode.REPO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getLatestFileContentDecodesCodeAndReturnsSha() {
        User user = User.builder()
                .id(1)
                .githubAccessToken("encrypted-token")
                .build();
        GithubRepo repo = GithubRepo.builder()
                .id(10)
                .organization("aivle")
                .name("BigP-Back")
                .build();
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 10))
                .thenReturn(Optional.of(UserRepo.builder().user(user).githubRepo(repo).build()));
        when(githubTokenCrypto.decrypt("encrypted-token")).thenReturn("github-token");
        String code = "class App {}";
        Map<String, Object> fileBody = Map.of(
                "content", Base64.getEncoder().encodeToString(code.getBytes(StandardCharsets.UTF_8)),
                "encoding", "base64",
                "sha", "latest-sha");
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                org.mockito.ArgumentMatchers
                        .<ParameterizedTypeReference<Map<String, Object>>>any()))
                .thenReturn(ResponseEntity.ok(fileBody));

        var response = service().getLatestFileContent(
                1, 10, "src/App.java", "feature/test");

        assertEquals(code, response.content());
        assertEquals("latest-sha", response.sha());
    }

    @Test
    void getPullRequestsReturnsMineAndMarksPlatformGenerated() {
        User user = User.builder()
                .id(1)
                .gitName("bose9029")
                .githubAccessToken("encrypted-token")
                .build();
        GithubRepo repo = GithubRepo.builder()
                .id(10)
                .organization("aivle")
                .name("BigP-Back")
                .build();
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 10))
                .thenReturn(Optional.of(UserRepo.builder().user(user).githubRepo(repo).build()));
        when(githubTokenCrypto.decrypt("encrypted-token")).thenReturn("github-token");
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                org.mockito.ArgumentMatchers
                        .<ParameterizedTypeReference<List<Map<String, Object>>>>any()))
                .thenReturn(ResponseEntity.ok(List.of(
                        pullRequest(42, "bose9029", "open", null),
                        pullRequest(41, "teammate", "closed", null))));
        when(githubPullRequestRepository.findPlatformGeneratedNumbers(10, Set.of(42)))
                .thenReturn(Set.of(42));

        var response = service().getRepositoryPullRequests(1, 10, "mine", "all", 1, 10);

        assertEquals(1, response.size());
        assertEquals(42, response.get(0).githubPrNumber());
        assertEquals("OPEN", response.get(0).status());
        assertEquals(true, response.get(0).platformGenerated());
    }

    @Test
    void getPullRequestsMapsMergedAndExternalPullRequest() {
        User user = User.builder()
                .id(1)
                .gitName("bose9029")
                .githubAccessToken("encrypted-token")
                .build();
        GithubRepo repo = GithubRepo.builder()
                .id(10)
                .organization("aivle")
                .name("BigP-Back")
                .build();
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 10))
                .thenReturn(Optional.of(UserRepo.builder().user(user).githubRepo(repo).build()));
        when(githubTokenCrypto.decrypt("encrypted-token")).thenReturn("github-token");
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                org.mockito.ArgumentMatchers
                        .<ParameterizedTypeReference<List<Map<String, Object>>>>any()))
                .thenReturn(ResponseEntity.ok(List.of(
                        pullRequest(40, "bose9029", "closed", "2026-07-28T11:00:00Z"))));
        when(githubPullRequestRepository.findPlatformGeneratedNumbers(10, Set.of(40)))
                .thenReturn(Set.of());

        var response = service().getRepositoryPullRequests(1, 10, "all", "closed", 1, 10);

        assertEquals("MERGED", response.get(0).status());
        assertEquals(false, response.get(0).platformGenerated());
    }

    @Test
    void getPullRequestsRejectsInvalidQuery() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> service().getRepositoryPullRequests(1, 10, "other", "all", 1, 10));

        assertEquals(ErrorCode.INVALID_PR_QUERY, exception.getErrorCode());
    }

    private Map<String, Object> pullRequest(
            int number,
            String author,
            String state,
            String mergedAt
    ) {
        Map<String, Object> pullRequest = new HashMap<>();
        pullRequest.put("number", number);
        pullRequest.put("title", "PR #" + number);
        pullRequest.put("state", state);
        pullRequest.put("html_url", "https://github.com/aivle/BigP-Back/pull/" + number);
        pullRequest.put("user", Map.of("login", author));
        pullRequest.put("head", Map.of("ref", "feature/" + number));
        pullRequest.put("base", Map.of("ref", "main"));
        pullRequest.put("created_at", "2026-07-27T09:00:00Z");
        pullRequest.put("updated_at", "2026-07-28T09:00:00Z");
        pullRequest.put("merged_at", mergedAt);
        return pullRequest;
    }

    private GithubService service() {
        return new GithubService(
                userRepository,
                githubTokenCrypto,
                githubRepoRepository,
                embeddingService,
                userRepoRepository,
                repoEmbeddingRepository,
                githubPullRequestRepository,
                analysisRepository,
                "https://example.com/webhook",
                "test-secret"
        ) {
            @Override
            RestTemplate restTemplate() {
                return restTemplate;
            }
        };
    }
}
