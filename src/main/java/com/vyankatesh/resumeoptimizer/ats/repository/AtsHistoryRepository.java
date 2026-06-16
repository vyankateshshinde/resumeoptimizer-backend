package com.vyankatesh.resumeoptimizer.ats.repository;

import com.vyankatesh.resumeoptimizer.ats.entity.AtsHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtsHistoryRepository extends JpaRepository<AtsHistoryEntity, Long> {

    List<AtsHistoryEntity> findByEmailOrderByCreatedAtDesc(String email);
}
