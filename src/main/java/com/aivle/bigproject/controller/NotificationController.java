package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.notification.NotificationResponse;
import com.aivle.bigproject.service.NotificationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getMyNotifications(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.getMyNotifications(Integer.valueOf(jwt.getSubject()));
    }

    @DeleteMapping("/{notification_id}")
    public ResponseEntity<Void> deleteOne(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer notification_id) {
        notificationService.deleteOne(Integer.valueOf(jwt.getSubject()), notification_id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAll(@AuthenticationPrincipal Jwt jwt) {
        notificationService.clearAll(Integer.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
}