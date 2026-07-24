package com.aivle.bigproject.service;

import com.aivle.bigproject.entity.Announcement;
import com.aivle.bigproject.entity.AnnouncementFile;
import com.aivle.bigproject.repository.AnnouncementFileRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.UUID;
import org.springframework.core.io.Resource;

@Service
@RequiredArgsConstructor
public class FileService {

    // 파일을 저장할 로컬 경로 (원하시는 경로로 설정 가능)
    private final String uploadDir = System.getProperty("user.dir") + "/uploads/announcements/";
    private final AnnouncementFileRepository announcementFileRepository; // 파일 레포지토리 주입

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

    public void deleteFile(AnnouncementFile fileEntity) {
        File file = new File(fileEntity.getFilePath());
        if (file.exists()) {
            file.delete();
        }
    }

    public AnnouncementFile findById(Integer fileId) {
        return announcementFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
    }

    public Resource loadAsResource(AnnouncementFile fileEntity) {
        try {
            Path filePath = Paths.get(fileEntity.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("파일을 읽을 수 없습니다: " + fileEntity.getOriginalFileName());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("파일 경로 오류: " + fileEntity.getOriginalFileName(), e);
        }
    }
}