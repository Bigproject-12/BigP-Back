package com.aivle.bigproject.dto.announcement;

import com.aivle.bigproject.entity.Announcement;
import java.time.LocalDateTime;

/**
 * 공지사항 상세 조회 응답 DTO + content 포함
 */


public record AnnouncementResponse(
        Integer boardId,
        Integer authorId,
        String title,
        String content,
        Integer viewCount,
        Boolean isPinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AnnouncementResponse from(Announcement announcement) {
        return new AnnouncementResponse(
                announcement.getBoardId(),
                announcement.getUser().getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getViewCount(),
                announcement.getIsPinned(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}