package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.analysis.AnalysisHistoryResponse;
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.UserRepoRepository;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    //레포 상세 - 히스토리 목록 조회
    @Transactional(readOnly = true)
    public List<AnalysisHistoryResponse> getHistory(Integer userId, Integer repoId) {
        userRepoRepository.findByUser_IdAndGithubRepo_Id(userId, repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));
        return analysisRepository.findHistoryByUserIdAndRepoId(userId, repoId);
    }
}
