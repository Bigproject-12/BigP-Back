package com.aivle.bigproject.service;

import com.aivle.bigproject.entity.Announcement;
import com.aivle.bigproject.entity.AnnouncementFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class FileService {

    // 파일을 저장할 로컬 경로 (원하시는 경로로 설정 가능)
    private final String uploadDir = System.getProperty("user.dir") + "/uploads/announcements/";

    public AnnouncementFile storeFile(MultipartFile file, Announcement announcement) {
        if (file.isEmpty()) return null;

        String originalFileName = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String storedFileName = uuid + "_" + originalFileName;
        String filePath = uploadDir + storedFileName;

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs(); // 디렉토리가 없으면 생성
        }

        try {
            file.transferTo(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + originalFileName, e);
        }

        return AnnouncementFile.builder()
                .announcement(announcement)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .filePath(filePath)
                .fileSize(file.getSize())
                .build();
    }
}