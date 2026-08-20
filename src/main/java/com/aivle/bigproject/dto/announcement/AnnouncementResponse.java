package com.aivle.bigproject.dto.announcement;

import com.aivle.bigproject.entity.Announcement;
import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime updatedAt,
        List<FileResponse> files // 📁 파일 목록 필드 추가
) {
    /**
     * Announcement 엔티티를 응답 DTO로 변환한다.
     */
    public static AnnouncementResponse from(Announcement announcement) {
        List<FileResponse> fileResponses = announcement.getFiles() != null
                ? announcement.getFiles().stream()
                        .map(file -> new FileResponse(
                                file.getFileId(), // 혹은 file.getId()
                                file.getOriginalFileName() // 혹은 file.getFileName() 등
                        ))
                        .toList()
                : List.of();
        
        return new AnnouncementResponse(
                announcement.getBoardId(),
                announcement.getUser().getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getViewCount(),
                announcement.getIsPinned(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt(),
                fileResponses
        );
    }
    /**
     * 공지사항 첨부파일 정보 DTO.
     */
        public record FileResponse(
            Integer fileId,
            String originalFileName
    ) {}
}

