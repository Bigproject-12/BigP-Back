package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Company;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
// 회사 정보를 관리하는 Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
    //회사명을 조회
    Optional<Company> findByNameIgnoreCase(String name);
    // 동일한 회사명이 존재하는지 확인 (업데이트 시 사용)
    boolean existsByNameIgnoreCase(String name);
    
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}
