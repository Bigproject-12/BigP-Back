package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.analysis.AnalysisHistoryResponse;
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.UserRepoRepository;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 레포지토리 분석 히스토리 조회 기능을 처리하는 Service
@Service
public class AnalysisHistoryService {
    private final AnalysisRepository analysisRepository;
    private final UserRepoRepository userRepoRepository;

    public AnalysisHistoryService(
            AnalysisRepository analysisRepository,
            UserRepoRepository userRepoRepository
    ) {
        this.analysisRepository = analysisRepository;
        this.userRepoRepository = userRepoRepository;
    }

    //레포 상세 - 사용자가 연결한 저장소의 분석 히스토리 목록 조회
    @Transactional(readOnly = true)
    public List<AnalysisHistoryResponse> getHistory(Integer userId, Integer repoId) {
        // 사용자가 해당 저장소에 연결되어 있는지 확인
        userRepoRepository.findByUser_IdAndGithubRepo_Id(userId, repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));
        return analysisRepository.findHistoryByUserIdAndRepoId(userId, repoId);
    }
}
