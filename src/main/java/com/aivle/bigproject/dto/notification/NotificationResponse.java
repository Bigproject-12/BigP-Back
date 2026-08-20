package com.aivle.bigproject.dto.notification;

import com.aivle.bigproject.entity.Notification;
import java.time.LocalDateTime;

/**
 * 사용자 알림 정보를 반환하기 위한 응답 DTO.
 * 특정 알림의 ID, 관련 분석/공지사항 ID, 알림 유형, 제목, 메시지, 읽음 여부, 생성일 등의 정보를 포함한다.
 */

public record NotificationResponse(
        Integer notificationId,
        Integer userId,
        Integer analysisId,
        Integer boardId,
        String type,
        String title,
        String message,
        Boolean read,
        LocalDateTime createdAt
) {
    /**
     * Notification 엔티티를 NotificationResponse DTO로 변환
     */
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUser().getId(),
                notification.getAnalysis() == null ? null :
                notification.getAnalysis().getId(),
                notification.getAnnouncement() == null ? null :
                notification.getAnnouncement().getBoardId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}
