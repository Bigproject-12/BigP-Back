package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.request.CompanyCreateRequest;
import com.aivle.bigproject.dto.request.CompanyUpdateRequest;
import com.aivle.bigproject.dto.response.CompanyResponse;
import com.aivle.bigproject.entity.Company;
import com.aivle.bigproject.repository.CompanyRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회사를 찾을 수 없습니다. id=" + id
                ));
    }
}
