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

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional
    public CompanyResponse create(CompanyCreateRequest request) {
        String name = normalizeName(request.name());
        validateDuplicateName(name);

        Company company = Company.builder()
                .name(name)
                .build();

        return CompanyResponse.from(companyRepository.save(company));
    }

    public Page<CompanyResponse> findAll(Pageable pageable) {
        return companyRepository.findAll(pageable)
                .map(CompanyResponse::from);
    }

    public CompanyResponse findById(Integer id) {
        return CompanyResponse.from(getCompany(id));
    }

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

    @Transactional
    public void delete(Integer id) {
        companyRepository.delete(getCompany(id));
    }

    private Company getCompany(Integer id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));
    }

    private void validateDuplicateName(String name) {
        if (companyRepository.existsByNameIgnoreCase(name)) {
            throw new CustomException(ErrorCode.COMPANY_ALREADY_EXISTS);
        }
    }

    private String normalizeName(String name) {
        return name.trim();
    }
}
