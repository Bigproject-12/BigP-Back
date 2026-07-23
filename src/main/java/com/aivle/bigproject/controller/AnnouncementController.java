package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.announcement.AnnouncementCreateRequest;
import com.aivle.bigproject.dto.announcement.AnnouncementResponse;
import com.aivle.bigproject.dto.announcement.AnnouncementSummaryResponse;
import com.aivle.bigproject.dto.announcement.AnnouncementUpdateRequest;
import com.aivle.bigproject.service.AnnouncementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /** 공지사항 게시 — 관리자 전용 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnnouncementResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("request") @Valid AnnouncementCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        AnnouncementResponse response =
                announcementService.create(Integer.valueOf(jwt.getSubject()), request, files);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** 공지사항 수정 — 관리자 전용 */
    @PatchMapping("/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnnouncementResponse> update(
            @PathVariable Integer noticeId,
            @Valid @RequestBody AnnouncementUpdateRequest request
    ) {
        return ResponseEntity.ok(announcementService.update(noticeId, request));
    }

    /** 공지사항 삭제 — 관리자 전용 */
    @DeleteMapping("/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer noticeId) {
        announcementService.delete(noticeId);
        return ResponseEntity.noContent().build();   // 204
    }

    /** 공지사항 상세 조회 — 로그인 사용자 전체 */
    @GetMapping("/{noticeId}")
    public ResponseEntity<AnnouncementResponse> getDetail(@PathVariable Integer noticeId) {
        return ResponseEntity.ok(announcementService.getDetail(noticeId));
    }

    /** 공지사항 목록 조회 — 검색 + 정렬 + 페이징 */
    @GetMapping
    public Page<AnnouncementSummaryResponse> getList(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return announcementService.getList(keyword, pageable);
    }
}