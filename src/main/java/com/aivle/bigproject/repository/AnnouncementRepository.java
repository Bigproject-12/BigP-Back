package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {

    // 고정된 공지사항을 최신순(작성일 역순)으로 먼저 조회하는 기능
    List<Announcement> findByIsPinnedOrderByCreatedAtDesc(Boolean isPinned);

    // 제목 · 내용 · 작성자명 통합 검색 + 페이징
    @Query(value = """
            SELECT a FROM Announcement a
            JOIN FETCH a.user u
            WHERE :keyword IS NULL
               OR a.title   LIKE %:keyword%
               OR a.content LIKE %:keyword%
               OR u.name    LIKE %:keyword%
            """,
            // 총 개수 조회용 — FETCH 없이 일반 JOIN
            countQuery = """
            SELECT COUNT(a) FROM Announcement a
            JOIN a.user u
            WHERE :keyword IS NULL
               OR a.title   LIKE %:keyword%
               OR a.content LIKE %:keyword%
               OR u.name    LIKE %:keyword%
            """)
    Page<Announcement> search(@Param("keyword") String keyword, Pageable pageable);
}