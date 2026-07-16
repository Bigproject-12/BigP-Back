package com.aivle.bigproject.dto.announcement;

import jakarta.validation.constraints.NotBlank;

/**
 * 공지사항 생성 요청 DTO (글쓰기)
 */

public record AnnouncementCreateRequest (
        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        Boolean isPinned // 상단 고정 여부 -> 선택
) {}