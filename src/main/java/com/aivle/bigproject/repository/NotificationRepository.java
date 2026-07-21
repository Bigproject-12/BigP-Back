
package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    List<Notification> findByUserIdOrderByIdDesc(Integer userId);
    List<Notification> findByUserIdAndIsReadFalse(Integer userId);

    @Modifying
    @Query(value = """
            DELETE FROM NOTIFICATION
            WHERE user_id = :userId
               OR analysis_id IN (
                   SELECT analysis_id FROM ANALYSIS WHERE user_id = :userId
               )
            """, nativeQuery = true)
    void deleteForAccount(@Param("userId") Integer userId);
}
