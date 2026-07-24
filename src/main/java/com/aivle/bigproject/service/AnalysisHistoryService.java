package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.analysis.AnalysisHistoryResponse;
import com.aivle.bigproject.repository.AnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnalysisHistoryService {
    private final AnalysisRepository analysisRepository;

    public AnalysisHistoryService(AnalysisRepository analysisRepository){
        this.analysisRepository = analysisRepository;
    }

    //레포 상세 - 히스토리 목록 조회
    @Transactional(readOnly = true)
    public List<AnalysisHistoryResponse> getHistory(Integer repoId) {
        return analysisRepository.findHistoryByRepoId(repoId);
    }
}
