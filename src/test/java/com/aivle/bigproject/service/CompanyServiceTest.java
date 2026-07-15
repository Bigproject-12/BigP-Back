package com.aivle.bigproject.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.bigproject.dto.company.CompanyCreateRequest;
import com.aivle.bigproject.dto.company.CompanyResponse;
import com.aivle.bigproject.dto.company.CompanyUpdateRequest;
import com.aivle.bigproject.entity.Company;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.CompanyRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        companyService = new CompanyService(companyRepository);
    }

    @Test
    void createNormalizesCompanyNameAndSavesCompany() {
        CompanyCreateRequest request = new CompanyCreateRequest("  AIVLE  ");
        Company savedCompany = Company.builder()
                .id(1)
                .name("AIVLE")
                .build();

        when(companyRepository.existsByNameIgnoreCase("AIVLE")).thenReturn(false);
        when(companyRepository.save(org.mockito.ArgumentMatchers.any(Company.class)))
                .thenReturn(savedCompany);

        CompanyResponse response = companyService.create(request);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.name()).isEqualTo("AIVLE");
        verify(companyRepository).existsByNameIgnoreCase("AIVLE");
    }

    @Test
    void createRejectsDuplicateCompanyName() {
        CompanyCreateRequest request = new CompanyCreateRequest("aivle");
        when(companyRepository.existsByNameIgnoreCase("aivle")).thenReturn(true);

        assertThatThrownBy(() -> companyService.create(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.COMPANY_ALREADY_EXISTS));

        verify(companyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void findByIdRejectsUnknownCompany() {
        when(companyRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.findById(999))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.COMPANY_NOT_FOUND));
    }

    @Test
    void updateRejectsAnotherCompanyWithSameName() {
        Company company = Company.builder()
                .id(1)
                .name("기존 회사")
                .build();
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameIgnoreCaseAndIdNot("중복 회사", 1))
                .thenReturn(true);

        assertThatThrownBy(() -> companyService.update(
                1,
                new CompanyUpdateRequest("중복 회사")
        ))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.COMPANY_ALREADY_EXISTS));

        assertThat(company.getName()).isEqualTo("기존 회사");
    }
}
