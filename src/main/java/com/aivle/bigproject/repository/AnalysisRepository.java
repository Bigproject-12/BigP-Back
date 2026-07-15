package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<Analysis, Integer> {
}