package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Integer> {
    boolean existsByCompanyId(Integer id);
}
