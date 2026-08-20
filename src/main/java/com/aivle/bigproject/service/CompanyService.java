package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.company.CompanyCreateRequest;
import com.aivle.bigproject.dto.company.CompanyResponse;
import com.aivle.bigproject.dto.company.CompanyUpdateRequest;
import com.aivle.bigproject.entity.Company;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.CompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// 회사 정보 등록, 수정, 삭제, 조회 기능을 처리하는 Service
@Service
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }
    // 새로운 회사 등록
    @Transactional
    public CompanyResponse create(CompanyCreateRequest request) {
        String name = normalizeName(request.name());
        validateDuplicateName(name);

        Company company = Company.builder()
                .name(name)
                .build();
        //회사 정보 저장 후 응답 DTO로 변환
        return CompanyResponse.from(companyRepository.save(company));
    }
    //회사 목록 페이징 조회
    public Page<CompanyResponse> findAll(Pageable pageable) {
        // 회사 목록을 조회하고 응답 DTO로 변환
        return companyRepository.findAll(pageable)
                .map(CompanyResponse::from);
    }
    //회사 ID를 기준으로 회사 정보 조회
    public CompanyResponse findById(Integer id) {
        return CompanyResponse.from(getCompany(id));
    }
    //회사 정보 수정
    @Transactional
    public CompanyResponse update(Integer id, CompanyUpdateRequest request) {
        Company company = getCompany(id);
        String name = normalizeName(request.name());

        if (companyRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new CustomException(ErrorCode.COMPANY_ALREADY_EXISTS);
        }

        company.updateName(name);
        return CompanyResponse.from(company);
    }
// 회사 정보 삭제
    @Transactional
    public void delete(Integer id) {
        companyRepository.delete(getCompany(id));
    }
    //회사 ID를 기준으로 엔티티 조회
    private Company getCompany(Integer id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));
    }
    // 회사 이름 중복 여부를 확인
    private void validateDuplicateName(String name) {
        if (companyRepository.existsByNameIgnoreCase(name)) {
            throw new CustomException(ErrorCode.COMPANY_ALREADY_EXISTS);
        }
    }
    // 회사명 등록시 앞뒤 공백 제거
    private String normalizeName(String name) {
        return name.trim();
    }
}
