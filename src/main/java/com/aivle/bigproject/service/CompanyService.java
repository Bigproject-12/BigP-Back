package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.company.CompanyCreateRequest;
import com.aivle.bigproject.dto.company.CompanyResponse;
import com.aivle.bigproject.dto.company.CompanyUpdateRequest;
import com.aivle.bigproject.entity.Company;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.CompanyRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional
    public CompanyResponse create(CompanyCreateRequest request) {
        Company company = Company.builder()
                .name(request.name())
                .build();

        return CompanyResponse.from(companyRepository.save(company));
    }

    public List<CompanyResponse> findAll() {
        return companyRepository.findAll().stream()
                .map(CompanyResponse::from)
                .toList();
    }

    public CompanyResponse findById(Integer id) {
        return CompanyResponse.from(getCompany(id));
    }

    @Transactional
    public CompanyResponse update(Integer id, CompanyUpdateRequest request) {
        Company company = getCompany(id);
        company.updateName(request.name());
        return CompanyResponse.from(company);
    }

    @Transactional
    public void delete(Integer id) {
        companyRepository.delete(getCompany(id));
    }

    private Company getCompany(Integer id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));
    }
}
