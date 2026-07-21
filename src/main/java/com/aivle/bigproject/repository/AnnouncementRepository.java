package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {
    
    // 고정된 공지사항을 최신순(작성일 역순)으로 먼저 조회하는 기능
    List<Announcement> findByIsPinnedOrderByCreatedAtDesc(Boolean isPinned);

    @Modifying
    @Query(value = "DELETE FROM ANNOUNCEMENT WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Integer userId);
}
