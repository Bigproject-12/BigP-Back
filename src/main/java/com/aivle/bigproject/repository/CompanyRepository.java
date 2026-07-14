package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Company;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository <Company, Integer> {
    Optional<Company> findById (String Id);
}