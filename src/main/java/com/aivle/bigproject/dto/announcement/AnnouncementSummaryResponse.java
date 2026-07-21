package com.aivle.bigproject.dto.announcement;

import com.aivle.bigproject.entity.Announcement;
import java.time.LocalDateTime;

/**
 * 공지사항 목록 조회 응답 DTO -> 목록 화면용
 */


public record AnnouncementSummaryResponse(
        Integer boardId,
        String title,
        Integer viewCount,
        Boolean isPinned,
        LocalDateTime createdAt
) {
    public static AnnouncementSummaryResponse from(Announcement announcement){
        return new AnnouncementSummaryResponse(
                announcement.getBoardId(),
                announcement.getTitle(),
                announcement.getViewCount(),
                announcement.getIsPinned(),
                announcement.getCreatedAt()
        );
    }
}
