package com.aivle.bigproject.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 공지사항 첨부파일 정보를 관리하는 엔티티
 * Announcement 엔티티와 연관관계를 가지며, 공지사항에 첨부된 파일의 원본 이름, 저장된 이름, 경로, 크기 등의 정보를 저장
 */
@Entity
@Table(name = "ANNOUNCEMENT_FILE")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AnnouncementFile {

    //공지 첨부파일 정보 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Integer fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Announcement announcement;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false)
    private String storedFileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;
}