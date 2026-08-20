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
     * 새로운 회사를 등록
     *  새로운 회사 정보 입력 시 중복 체크 후 새로운 회사 정보를 생성
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

        /**
     * 등록된 회사 목록 조회
     *
     * 조회시 기본 목록 갯수는 20개이며, 회사 ID를 기준으로 정렬.
     */
    @GetMapping
    public Page<CompanyResponse> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return companyService.findAll(pageable);
    }

    /**
     * 특정 회사 ID를 기준으로 회사 정보 조회
     */
    @GetMapping("/{id}")
    public CompanyResponse findById(@PathVariable Integer id) {
        return companyService.findById(id);
    }

    /**
     * 회사 ID를 기준으로 기존 회사 정보를 수정
     *
     * 요청받은 회사 정보를 검증한 후 해당 회사의 정보를 업데이트하고, 업데이트된 회사 정보를 반환
     */
    @PutMapping("/{id}")
    public CompanyResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody CompanyUpdateRequest request
    ) {
        return companyService.update(id, request);
    }

    /**
     * 회사 ID를 기준으로 회사 정보를 삭제
     *
     * 해당 회사 정보를 데이터베이스에서 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
