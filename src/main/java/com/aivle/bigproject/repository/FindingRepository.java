package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<Finding, Integer> {
}