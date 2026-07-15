package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {
    
    // 고정된 공지사항을 최신순(작성일 역순)으로 먼저 조회하는 기능
    List<Announcement> findByIsPinnedOrderByCreatedAtDesc(Boolean isPinned);
}