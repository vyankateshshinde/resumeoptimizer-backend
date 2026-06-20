package com.vyankatesh.resumeoptimizer.resumeversion.repository;

import com.vyankatesh.resumeoptimizer.resumeversion.entity.ResumeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeTemplateRepository extends JpaRepository<ResumeTemplate, Long> {

    List<ResumeTemplate> findByActiveTrueOrderByMarketFitScoreDesc();
}