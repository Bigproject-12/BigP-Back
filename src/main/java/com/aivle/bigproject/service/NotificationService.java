package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.notification.NotificationResponse;
import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Announcement;
import com.aivle.bigproject.entity.Notification;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.NotificationRepository;
import com.aivle.bigproject.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // 내 알림 목록
    public List<NotificationResponse> getMyNotifications(Integer userId) {
        return notificationRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    // 알림 개별 삭제 ( 본인 것만 )
    @Transactional
    public void deleteOne(Integer userId, Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
        notificationRepository.delete(notification);
    }

    // 알림 전체 비우기(삭제)
    @Transactional
    public void clearAll(Integer userId) {
        notificationRepository.deleteAllForUser(userId);
    }

    //  코드 분석 완료 시 사용자에게 알림 생성
    @Transactional
    public void notifyAnalysisCompleted(Analysis analysis) {
        Notification notification = Notification.builder()
                .user(analysis.getUser())
                .analysis(analysis)
                .type("ANALYSIS_COMPLETE")
                .title("코드 분석이 완료되었습니다")
                .message(analysis.getLanguage() + " 코드 분석 결과를 확인해보세요.")
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    // 관리자가 공지사항 등록 시 전체 사용자에게 알림
    @Transactional
    public void notifyAnnouncementCreated(Announcement announcement) {
        List<User> allUsers = userRepository.findAll();
        List<Notification> notifications = allUsers.stream()
                .map(u -> Notification.builder()
                        .user(u)
                        .analysis(null)
                        .announcement(announcement)
                        .type("ANNOUNCEMENT")
                        .title("새 공지사항이 등록되었습니다")
                        .message(announcement.getTitle())
                        .isRead(false)
                        .build())
                .toList();
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void markOneAsRead(Integer userId, Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
        notification.setIsRead(true);
}

    // 알림 전체 읽음 처리
    /* @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsRead(userId);
    } */
}