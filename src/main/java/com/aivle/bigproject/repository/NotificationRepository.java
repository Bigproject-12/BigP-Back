
package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

// Notification 정보를 관리하는 Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    // 사용자의 알림을 최신순으로 조회
    List<Notification> findByUserIdOrderByIdDesc(Integer userId);
    // 사용자의 읽지 않은 알림 조회
    List<Notification> findByUserIdAndIsReadFalse(Integer userId);
    // 회원 탈퇴 시 사용자 및 분석 관련 알림 삭제
    @Modifying
    @Query(value = """
            DELETE FROM NOTIFICATION
            WHERE user_id = :userId
               OR analysis_id IN (
                   SELECT analysis_id FROM ANALYSIS WHERE user_id = :userId
               )
            """, nativeQuery = true)
    void deleteForAccount(@Param("userId") Integer userId);
    // 사용자의 모든 알림 삭제
    @Modifying
    @Query(value = "DELETE FROM NOTIFICATION WHERE user_id = :userId", nativeQuery = true)
    void deleteAllForUser(@Param("userId") Integer userId);
    // 사용자의 읽지 않은 모든 알림을 읽음 처리
    @Modifying
    @Query(value = "UPDATE NOTIFICATION SET is_read = true WHERE user_id = :userId AND is_read = false", nativeQuery =true)
    void markAllAsRead(@Param("userId") Integer userId);
    // 특정 공지사항과 관련된 알림 삭제
    @Modifying
    @Query(value = "DELETE FROM NOTIFICATION WHERE board_id = :boardId", nativeQuery = true)
    void deleteByAnnouncement(@Param("boardId") Integer boardId);
}
