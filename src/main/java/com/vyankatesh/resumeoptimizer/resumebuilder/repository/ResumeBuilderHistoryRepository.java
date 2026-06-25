
package com.vyankatesh.resumeoptimizer.resumebuilder.repository;

import com.vyankatesh.resumeoptimizer.resumebuilder.entity.ResumeBuilderHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeBuilderHistoryRepository extends JpaRepository<ResumeBuilderHistory, Long> {

    List<ResumeBuilderHistory> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}