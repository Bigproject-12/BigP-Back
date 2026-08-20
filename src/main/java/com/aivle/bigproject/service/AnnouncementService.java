package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.announcement.AnnouncementCreateRequest;
import com.aivle.bigproject.dto.announcement.AnnouncementResponse;
import com.aivle.bigproject.dto.announcement.AnnouncementSummaryResponse;
import com.aivle.bigproject.dto.announcement.AnnouncementUpdateRequest;
import com.aivle.bigproject.entity.Announcement;
import com.aivle.bigproject.entity.AnnouncementFile;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.AnnouncementRepository;
import com.aivle.bigproject.repository.NotificationRepository;
import com.aivle.bigproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;


// 공지사항 등록, 수정, 삭제, 조회 기능을 처리하는 Service
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // 기본은 읽기 전용, 쓰기 메서드만 덮어씀
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final FileService fileService;

    private final ViewCountGuard viewCountGuard;

    //공지 등록
    @Transactional
    public AnnouncementResponse create(Integer userId, AnnouncementCreateRequest request, List<MultipartFile> files) {
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Announcement saved = announcementRepository.save(
                Announcement.builder()
                        .user(writer)
                        .title(request.title())
                        .content(request.content())
                        // 고정 여부는 선택 입력 → null이면 false
                        .isPinned(Boolean.TRUE.equals(request.isPinned()))
                        .build()
        );


        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    AnnouncementFile announcementFile = fileService.storeFile(file, saved);
                    saved.getFiles().add(announcementFile);
                }
            }
        }
        notificationService.notifyAnnouncementCreated(saved);
        return AnnouncementResponse.from(saved);
    }

    //공지 수정 — save() 없이 더티 체킹으로 반영, 첨부파일 삭제/추가 포함
    @Transactional
    public AnnouncementResponse update(Integer boardId, AnnouncementUpdateRequest request, List<MultipartFile> files) {
        Announcement announcement = findOrThrow(boardId);
        announcement.update(request.title(), request.content(), request.isPinned());

        if (request.deleteFileIds() != null && !request.deleteFileIds().isEmpty()) {
            List<AnnouncementFile> toDelete = announcement.getFiles().stream()
                    .filter(f -> request.deleteFileIds().contains(f.getFileId()))
                    .toList();
            toDelete.forEach(fileService::deleteFile);
            announcement.getFiles().removeAll(toDelete); // orphanRemoval=true → DB에서도 삭제됨
        }

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    AnnouncementFile announcementFile = fileService.storeFile(file, announcement);
                    announcement.getFiles().add(announcementFile);
                }
            }
        }

        return AnnouncementResponse.from(announcement);
    }

    // 공지 삭제
    @Transactional
    public void delete(Integer boardId) {
        Announcement announcement = findOrThrow(boardId);
        announcement.getFiles().forEach(fileService::deleteFile);
        notificationRepository.deleteByAnnouncement(boardId);
        announcementRepository.delete(announcement);
    }

    // 상세 조회
    @Transactional
    public AnnouncementResponse getDetail(Integer userId, Integer boardId) {
        Announcement announcement = findOrThrow(boardId);

        // 1시간 내 같은 사용자의 재조회는 집계하지 않음
        if (viewCountGuard.shouldIncrease(userId, boardId)) {
            announcement.increaseViewCount();
        }
        return AnnouncementResponse.from(announcement);
    }

    //목록 조회 (검색 + 페이징)
    public Page<AnnouncementSummaryResponse> getList(String keyword, Pageable pageable) {
        // 빈 문자열·공백만 들어온 경우 검색어 없음(null)으로 통일
        String normalized = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        // 고정 공지는 항상 위로 오도록 isPinned를 첫 정렬 조건으로 추가
        Sort pinnedFirst = Sort.by(Sort.Direction.DESC, "isPinned")
                .and(pageable.getSort());

        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pinnedFirst
        );

        return announcementRepository.search(normalized,sorted)
                .map(AnnouncementSummaryResponse::from);
    }

    // 공통 조회 — 없으면 예외
    private Announcement findOrThrow(Integer boardId) {
        return announcementRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
    }
}