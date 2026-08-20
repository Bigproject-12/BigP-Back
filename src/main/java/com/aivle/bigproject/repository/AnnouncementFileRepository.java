package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.AnnouncementFile;
import org.springframework.data.jpa.repository.JpaRepository;

// 공지사항 첨부 파일을 관리하는 Repository
public interface AnnouncementFileRepository extends JpaRepository<AnnouncementFile, Integer> {
}