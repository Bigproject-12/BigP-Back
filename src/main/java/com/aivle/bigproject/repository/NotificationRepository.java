/* 
package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    List<Notification> findByUserIdOrderByIdDesc(Integer userId);
    List<Notification> findByUserIdAndIsReadFalse(Integer userId);
}
    */