package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.notification.NotificationResponse;
import com.aivle.bigproject.service.NotificationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 알림 관련 API를 제공하는 REST Controller.
 */
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 사용자의 알림 목록을 조회한다.
     */
    @GetMapping
    public List<NotificationResponse> getMyNotifications(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.getMyNotifications(Integer.valueOf(jwt.getSubject()));
    }

    /**
     * 특정 알림을 삭제한다.
     */
    @DeleteMapping("/{notification_id}")
    public ResponseEntity<Void> deleteOne(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer notification_id) {
        notificationService.deleteOne(Integer.valueOf(jwt.getSubject()), notification_id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자의 모든 알림을 삭제한다.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearAll(@AuthenticationPrincipal Jwt jwt) {
        notificationService.clearAll(Integer.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    /**
     * 특정 알림을 읽음 상태로 변경한다.
     */
    @PatchMapping("/{notification_id}/read")
    public ResponseEntity<Void> markOneAsRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer notification_id) {
        notificationService.markOneAsRead(Integer.valueOf(jwt.getSubject()), notification_id);
        return ResponseEntity.noContent().build();
    }

    /* @PatchMapping("/read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsRead(Integer.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    } */
}