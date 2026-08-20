package com.aivle.bigproject.dto.announcement;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 공지사항 수정 요청 DTO (글수정)
 * PATCH — null인 필드는 수정하지 않음
 */
public record AnnouncementUpdateRequest(
        @Size(min = 1, max = 255, message = "제목은 1~255자여야 합니다.")
        String title,

        @Size(min = 1, message = "내용은 비어 있을 수 없습니다.")
        String content,

        Boolean isPinned,

        // 삭제할 기존 첨부파일 ID 목록 (선택)
        List<Integer> deleteFileIds
) {}