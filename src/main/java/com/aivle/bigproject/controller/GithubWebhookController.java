package com.aivle.bigproject.controller;

import com.aivle.bigproject.service.GithubService;
import com.aivle.bigproject.service.GithubPullRequestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
public class GithubWebhookController {

    private final GithubService githubService;
    private final JsonMapper jsonMapper;
    private final GithubPullRequestService githubPullRequestService;

    @Value("${github.webhook-secret}")
    private String webhookSecret;

    public GithubWebhookController(
            GithubService githubService,
            JsonMapper jsonMapper,
            GithubPullRequestService githubPullRequestService
    ) {
        this.githubService = githubService;
        this.jsonMapper = jsonMapper;
        this.githubPullRequestService = githubPullRequestService;
    }

    @PostMapping("/github")
    public ResponseEntity<String> handleGithubWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestBody String rawPayload
    ) {
        if (signature == null || !isValidSignature(rawPayload, signature)) {
            return ResponseEntity.status(401).body("서명 검증 실패");
        }

        log.info("Webhook 수신: " + eventType);

        if ("ping".equals(eventType)) {
            return ResponseEntity.ok("pong");
        }

        if ("push".equals(eventType)) {
            log.info("Push 이벤트 발생");
            handlePushEvent(rawPayload);
            return ResponseEntity.ok("received");
        }

        if ("organization".equals(eventType)) {
            handleOrganizationEvent(rawPayload);
            return ResponseEntity.ok("received");
        }

        if ("pull_request".equals(eventType)) {
            handlePullRequestEvent(rawPayload);
            return ResponseEntity.ok("received");
        }

        return ResponseEntity.ok("ignored");
    }

    private void handlePullRequestEvent(String rawPayload) {
        JsonNode json = jsonMapper.readTree(rawPayload);
        String action = json.get("action").asString();
        if (!"opened".equals(action) && !"reopened".equals(action) && !"closed".equals(action)) {
            return;
        }

        JsonNode pullRequest = json.get("pull_request");
        String status;
        LocalDateTime mergedAt = null;
        if ("closed".equals(action) && pullRequest.get("merged").asBoolean()) {
            status = "MERGED";
            JsonNode mergedAtNode = pullRequest.get("merged_at");
            if (mergedAtNode != null && !mergedAtNode.isNull()) {
                mergedAt = OffsetDateTime.parse(mergedAtNode.asString())
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime();
            }
        } else if ("closed".equals(action)) {
            status = "CLOSED";
        } else {
            status = "OPEN";
        }

        boolean updated = githubPullRequestService.synchronizeStatus(
                json.get("repository").get("owner").get("login").asString(),
                json.get("repository").get("name").asString(),
                pullRequest.get("number").asInt(),
                status,
                mergedAt
        );
        if (!updated) {
            log.info("저장되지 않은 PR Webhook은 무시합니다: {}/{} #{}",
                    json.get("repository").get("owner").get("login").asString(),
                    json.get("repository").get("name").asString(),
                    pullRequest.get("number").asInt());
        }
    }

    private void handleOrganizationEvent(String rawPayload) {
        JsonNode json = jsonMapper.readTree(rawPayload);
        String action = json.get("action").asString();

        if ("member_removed".equals(action)) {
            String orgName = json.get("organization").get("login").asString();
            String removedGitId = json.get("membership").get("user").get("id").asString();
            String removedGitLogin = json.get("membership").get("user").get("login").asString();

            log.info("{} 조직에서 {} 님(id={})이 추방됨을 감지했습니다.", orgName, removedGitLogin, removedGitId);
            githubService.revokeOrgAccess(orgName, removedGitId);
        }
    }

    private boolean isValidSignature(String payload, String signatureHeader) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = "sha256=" + HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    private void handlePushEvent(String rawPayload) {
        JsonNode json = jsonMapper.readTree(rawPayload);
        String ref = json.get("ref").asString();
        String repoName = json.get("repository").get("name").asString();
        String defaultBranch = "dev";

        JsonNode orgNode = json.get("organization");
        String orgName = orgNode != null
                ? orgNode.get("login").asString()
                : json.get("repository").get("owner").get("login").asString();

        if (!ref.equals("refs/heads/" + defaultBranch)) {
            log.info("기본 브랜치({})가 아닌 {}로의 push라 재임베딩 스킵", defaultBranch, ref);
            return;
        }

        Set<String> addedPaths = new HashSet<>();
        Set<String> modifiedPaths = new HashSet<>();
        Set<String> removedPaths = new HashSet<>();

        for (JsonNode commit : json.get("commits")) {
            commit.get("added").forEach(f -> addedPaths.add(f.asString()));
            commit.get("modified").forEach(f -> modifiedPaths.add(f.asString()));
            commit.get("removed").forEach(f -> removedPaths.add(f.asString()));
        }

        log.info("{}/{} {} 브랜치 push 감지 — added:{}, modified:{}, removed:{}",
                orgName, repoName, defaultBranch, addedPaths.size(), modifiedPaths.size(), removedPaths.size());

        githubService.processPushEmbedding(orgName, repoName, defaultBranch, addedPaths, modifiedPaths, removedPaths);
    }
}
