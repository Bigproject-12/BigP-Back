package com.aivle.bigproject.controller;

import com.aivle.bigproject.service.GithubService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
public class GithubWebhookController {

    private final GithubService githubService;
    private final JsonMapper jsonMapper;

    @Value("${github.webhook-secret}")
    private String webhookSecret;

    public GithubWebhookController(GithubService githubService, JsonMapper jsonMapper) {
        this.githubService = githubService;
        this.jsonMapper = jsonMapper;
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
            return ResponseEntity.ok("received");
        }

        if ("organization".equals(eventType)) {
            handleOrganizationEvent(rawPayload);
            return ResponseEntity.ok("received");
        }

        return ResponseEntity.ok("ignored");
    }

    private void handleOrganizationEvent(String rawPayload) {
        JsonNode json = jsonMapper.readTree(rawPayload);
        String action = json.get("action").asText();

        if ("member_removed".equals(action)) {
            String orgName = json.get("organization").get("id").asText();
            String removedGitId = json.get("membership").get("user").get("login").asText();
            String removedGitLogin = json.get("membership").get("user").get("login").asText();

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
}