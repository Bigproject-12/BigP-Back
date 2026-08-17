package com.aivle.bigproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 공지사항 정보를 관리하는 엔티티 
 * 관리자가 작성한 공지사항 정보를 저장하고, 조회, 수정, 삭제 등의 기능을 제공
 * 공지사항은 제목, 내용, 작성자, 조회수, 고정 여부 등의 정보를 포함
 * Announcement
 */
@Entity
@Table(name = "ANNOUNCEMENT")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Integer boardId;

    // 💡 연관관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    /**
     * 공지사항에 첨부된 파일 목록을 관리하는 연관관계 매핑
     * AnnouncementFile 엔티티와 1:N 관계를 가지며, 공지사항 삭제 시 첨부파일도 함께 삭제되도록 설정
     */
    @Builder.Default
    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnnouncementFile> files = new ArrayList<>();

    /**
     * 공지 사항 정보를 수정하는 메서드
     * 제목, 내용, 고정 여부를 선택적으로 수정할 수 있도록 구현
     */
    public void update(String title, String content, Boolean isPinned) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (isPinned != null) this.isPinned = isPinned;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }
}