package com.aivle.bigproject.ai.service;

// DTO
import com.aivle.bigproject.ai.dto.DetectRequest;
import com.aivle.bigproject.ai.dto.DetectResponse;

// Entity
import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Company;
import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.User;

// Repository
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.CompanyRepository;
import com.aivle.bigproject.repository.GithubRepoRepository;
import com.aivle.bigproject.repository.UserRepository;

// Exception
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;

// Spring Web
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aivle.bigproject.repository.FindingRepository;
import com.aivle.bigproject.entity.Finding;

@Service
public class AnalysisService {

    // AI 서버 주소
    private final String AI_DETECT_URL = "http://localhost:8000/api/ai/detect";

    private final AnalysisRepository analysisRepository;
    private final GithubRepoRepository githubRepoRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    private final FindingRepository findingRepository;
    private final ObjectMapper objectMapper;

    public AnalysisService(AnalysisRepository analysisRepository, 
                           GithubRepoRepository githubRepoRepository, 
                           CompanyRepository companyRepository, 
                           UserRepository userRepository,
                           FindingRepository findingRepository,
                           ObjectMapper objectMapper) {
        this.analysisRepository = analysisRepository;
        this.githubRepoRepository = githubRepoRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.findingRepository = findingRepository;
        this.objectMapper = objectMapper;
    }

    // 통신 실패 시 DB 롤백 방지를 위해 @Transactional은 일부러 생략합니다.
    public DetectResponse sendToAiServer(DetectRequest requestDto) {

        // 1. 필요한 엔티티 조회
        GithubRepo repo = githubRepoRepository.findById(requestDto.repoId())
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));
        Company company = companyRepository.findById(requestDto.companyId())
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));
        User user = userRepository.findById(requestDto.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 분석 내역을 '분석 중' 상태로 DB에 최초 저장
        Analysis analysis = Analysis.builder()
                .githubRepo(repo)
                .company(company)
                .user(user)
                .originCode(requestDto.codeContent())
                .language(requestDto.language())
                .prompt(null)
                .status("ANALYZING") // 초기 생성 시 곧바로 ANALYZING 처리
                .build();
                
        analysisRepository.save(analysis);

        // 3. FastAPI 서버로 HTTP 요청 준비
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<DetectRequest> requestEntity = new HttpEntity<>(requestDto, headers);

        // 4. 요청 및 결과에 따른 상태 업데이트
        try {
            DetectResponse response = restTemplate.postForObject(AI_DETECT_URL, requestEntity, DetectResponse.class);
            
            // 통신 성공 시 '완료' 상태로 업데이트
            analysis.setStatus("COMPLETED");
            analysisRepository.save(analysis);
            
            int securityCount = response.vulnerabilities() != null ? response.vulnerabilities().size() : 0;
            int inefficiencyCount = response.complexityDetails() != null ? response.complexityDetails().size() : 0;
            
            if (securityCount > 0 || inefficiencyCount > 0) {
                
                String secuResultStr = objectMapper.writeValueAsString(response.vulnerabilities());
                String inefficiencyResultStr = objectMapper.writeValueAsString(response.complexityDetails());
                String modifiedCode = response.patchedCode() != null ? response.patchedCode() : "";

                Finding finding = Finding.builder()
                        .analysis(analysis)
                        .inefficiencyResult(inefficiencyResultStr)
                        .modifiedCode(modifiedCode)
                        .secuResult(secuResultStr)
                        .duplicateResult("[]") 
                        .isAiGenerated(response.isAiGenerated() != null && response.isAiGenerated())
                        .totalIssues(securityCount + inefficiencyCount)
                        .securityCount(securityCount)
                        .inefficiencyCount(inefficiencyCount)
                        .build();

                findingRepository.save(finding);
            }
            else{
                System.out.println("발견된 문제 없어서 FINDING엔 올라갈 게 없음")
            }
            
            return response;
            
        } catch (Exception e) {
            // 통신 실패 시 '실패' 상태로 업데이트
            analysis.setStatus("FAILED");
            analysisRepository.save(analysis);
            e.printStackTrace();
            throw new RuntimeException("AI 서버 분석 요청에 실패했습니다.");
        }
    }
}