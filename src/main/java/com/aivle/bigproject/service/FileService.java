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
//공지사항 첨부파일의 저장, 삭제, 조회 및 다운로드를 처리하는 Service
@Service
@RequiredArgsConstructor
public class FileService {

    // 파일을 저장할 로컬 경로 (사용자가 원하는 경로로 설정 가능)
    private final String uploadDir = System.getProperty("user.dir") + "/uploads/announcements/";
    private final AnnouncementFileRepository announcementFileRepository; // 파일 레포지토리 주입
    //업로드된 첨부파일을 로컬에 저장하고 파일 정보 생성
    public AnnouncementFile storeFile(MultipartFile file, Announcement announcement) {
        // 내부가 빈 파일의 경우 저장하지 않음
        if (file.isEmpty()) return null;
        // 원본 파일명 조회
        String originalFileName = file.getOriginalFilename();
        // 파일명 중복 방지를 위한 UUID 생성
        String uuid = UUID.randomUUID().toString();
        // 실제 파일 저장 경로 생성
        String storedFileName = uuid + "_" + originalFileName;
        // 실제 파일 저장 경로 생성
        String filePath = uploadDir + storedFileName;
        // 저장 디렉토리가 없으면 생성
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs(); // 디렉토리가 없으면 생성
        }
        // 파일을 로컬 디렉토리에 저장
        try {
            file.transferTo(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + originalFileName, e);
        }
        // 저장된 파일 정보를 엔티티로 생성
        return AnnouncementFile.builder()
                .announcement(announcement)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .filePath(filePath)
                .fileSize(file.getSize())
                .build();
    }
    //로컬에 저장된 첨부파일 삭제
    public void deleteFile(AnnouncementFile fileEntity) {
        File file = new File(fileEntity.getFilePath());
        if (file.exists()) {
            file.delete();
        }
    }
//파일 ID를 기준으로 첨부파일 정보 조회
    public AnnouncementFile findById(Integer fileId) {
        return announcementFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
    }
    //다운로드할 첨부파일을 Resource 형태로 조회
    public Resource loadAsResource(AnnouncementFile fileEntity) {
        try {
            // 저장된 파일 경로를 Resource로 변환
            Path filePath = Paths.get(fileEntity.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            // 파일 존재 및 읽기 가능 여부 확인
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