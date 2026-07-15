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

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

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
