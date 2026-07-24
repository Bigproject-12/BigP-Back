package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.announcement.AnnouncementCreateRequest;
import com.aivle.bigproject.dto.announcement.AnnouncementResponse;
import com.aivle.bigproject.dto.announcement.AnnouncementSummaryResponse;
import com.aivle.bigproject.dto.announcement.AnnouncementUpdateRequest;
import com.aivle.bigproject.service.AnnouncementService;
import com.aivle.bigproject.service.FileService;

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
import org.springframework.web.util.UriUtils;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import org.springframework.web.util.UriUtils;
import com.aivle.bigproject.entity.AnnouncementFile;
import com.aivle.bigproject.service.FileService;
import com.aivle.bigproject.entity.AnnouncementFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final FileService fileService;

    public AnnouncementController(AnnouncementService announcementService, FileService fileService) {
        this.announcementService = announcementService;
        this.fileService = fileService;
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

    //공지사항 수정 — 관리자 전용
    @PatchMapping("/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnnouncementResponse> update(
            @PathVariable Integer noticeId,
            @Valid @RequestBody AnnouncementUpdateRequest request
    ) {
        return ResponseEntity.ok(announcementService.update(noticeId, request));
    }

    //공지사항 삭제 — 관리자 전용
    @DeleteMapping("/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer noticeId) {
        announcementService.delete(noticeId);
        return ResponseEntity.noContent().build();   // 204
    }

    //공지사항 상세 조회 — 로그인 사용자 전체
    @GetMapping("/{noticeId}")
    public ResponseEntity<AnnouncementResponse> getDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer noticeId
    ) {
        return ResponseEntity.ok(
                announcementService.getDetail(Integer.valueOf(jwt.getSubject()), noticeId)
        );
    }

    // 공지사항 목록 조회 — 검색 + 정렬 + 페이징
    @GetMapping
    public Page<AnnouncementSummaryResponse> getList(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return announcementService.getList(keyword, pageable);
    }

    /*첨부파일 다운로드 - 로그인 사용자 전체*/
    @GetMapping("/files/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Integer fileId) {
        AnnouncementFile fileEntity = fileService.findById(fileId);
        Resource resource = fileService.loadAsResource(fileEntity);

        // 한글 파일명이 깨지지 않도록 유니코드 인코딩 처리
        String encodedFileName = UriUtils.encode(fileEntity.getOriginalFileName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                .body(resource);
    }
}