package com.aivle.bigproject.dto.company;

import com.aivle.bigproject.entity.Company;
import java.time.LocalDate;

/**
 * 회사 정보를 프론트에 반환하기 위한 응답 DTO.
 * 회사명, 생성일, 수정일 등의 정보를 포함되어 프론트로 전달
 */
public record CompanyResponse(
        Integer id,
        String name,
        LocalDate createdAt,
        LocalDate updatedAt
) {
    /**
     * Company 엔티티를 CompanyResponse DTO로 변환
     */
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
