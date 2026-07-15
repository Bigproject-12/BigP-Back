package com.aivle.bigproject.dto.company;

import com.aivle.bigproject.entity.Company;
import java.time.LocalDate;

public record CompanyResponse(
        Integer id,
        String name,
        LocalDate createdAt,
        LocalDate updatedAt
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
