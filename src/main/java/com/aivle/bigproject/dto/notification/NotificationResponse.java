package com.aivle.bigproject.dto.notification;

import com.aivle.bigproject.entity.Notification;
import java.time.LocalDateTime;

/**
 * 알림 조회 응답 DTO
 */

public record NotificationResponse(
        Integer id,
        Integer userId,
        Integer analysisId,
        String type,
        String title,
        String message,
        Boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUser().getId(),
                notification.getAnalysis() == null ? null :
                notification.getAnalysis().getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}
