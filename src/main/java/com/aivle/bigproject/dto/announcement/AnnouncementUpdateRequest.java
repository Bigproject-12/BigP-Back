package com.aivle.bigproject.dto.announcement;

import jakarta.validation.constraints.NotBlank;

/**
 * 공지사항 수정 요청 DTO (글수정)
 */

public record AnnouncementUpdateRequest(
        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        Boolean isPinned
) {}
