package com.aivle.bigproject.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.bigproject.service.GithubPullRequestService;
import com.aivle.bigproject.service.GithubService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class GithubWebhookControllerTest {

    private static final String SECRET = "webhook-test-secret";

    @Mock GithubService githubService;
    @Mock GithubPullRequestService githubPullRequestService;
    private GithubWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new GithubWebhookController(
                githubService, JsonMapper.builder().build(), githubPullRequestService);
        ReflectionTestUtils.setField(controller, "webhookSecret", SECRET);
        when(githubPullRequestService.synchronizeStatus(
                any(), any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    void synchronizesMergedPullRequest() throws Exception {
        String payload = payload("closed", true, "2026-07-29T01:30:00Z");

        var response = controller.handleGithubWebhook(
                signature(payload), "pull_request", payload);

        assertEquals(200, response.getStatusCode().value());
        verify(githubPullRequestService).synchronizeStatus(
                eq("aivle"), eq("BigP-Back"), eq(42), eq("MERGED"),
                any(LocalDateTime.class));
    }

    @Test
    void synchronizesClosedPullRequest() throws Exception {
        String payload = payload("closed", false, null);

        controller.handleGithubWebhook(signature(payload), "pull_request", payload);

        verify(githubPullRequestService).synchronizeStatus(
                "aivle", "BigP-Back", 42, "CLOSED", null);
    }

    @Test
    void synchronizesReopenedPullRequest() throws Exception {
        String payload = payload("reopened", false, null);

        controller.handleGithubWebhook(signature(payload), "pull_request", payload);

        verify(githubPullRequestService).synchronizeStatus(
                eq("aivle"), eq("BigP-Back"), eq(42), eq("OPEN"), isNull());
    }

    private String payload(String action, boolean merged, String mergedAt) {
        String mergedAtJson = mergedAt == null ? "null" : "\"" + mergedAt + "\"";
        return """
                {
                  "action": "%s",
                  "repository": {
                    "name": "BigP-Back",
                    "owner": { "login": "aivle" }
                  },
                  "pull_request": {
                    "number": 42,
                    "merged": %s,
                    "merged_at": %s
                  }
                }
                """.formatted(action, merged, mergedAtJson);
    }

    private String signature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + java.util.HexFormat.of()
                .formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
