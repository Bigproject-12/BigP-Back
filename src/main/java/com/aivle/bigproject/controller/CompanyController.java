package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.company.CompanyCreateRequest;
import com.aivle.bigproject.dto.company.CompanyResponse;
import com.aivle.bigproject.dto.company.CompanyUpdateRequest;
import com.aivle.bigproject.service.CompanyService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회사(Company) 관련 API를 제공하는 REST Controller.
 *
 * 회사 정보의 생성, 전체 조회, 상세 조회, 수정 및 삭제 기능을 제공
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * 새로운 회사를 등록한다.
     * @param request 회사 등록 요청 DTO
     * @return 등록된 회사 정보와 함께 201 Created 상태 코드 반환
     * @throws IllegalArgumentException 요청 DTO가 유효하지 않은 경우 발생
     * @throws RuntimeException 회사 등록 중 오류가 발생한 경우 발생
     * @throws Exception 기타 예외 발생 시 처리
     */
    @PostMapping
    public ResponseEntity<CompanyResponse> create(
            @Valid @RequestBody CompanyCreateRequest request
    ) {
        CompanyResponse response = companyService.create(request);
        return ResponseEntity
                .created(URI.create("/api/companies/" + response.id()))
                .body(response);
    }

    @GetMapping
    public Page<CompanyResponse> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return companyService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public CompanyResponse findById(@PathVariable Integer id) {
        return companyService.findById(id);
    }

    @PutMapping("/{id}")
    public CompanyResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody CompanyUpdateRequest request
    ) {
        return companyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
