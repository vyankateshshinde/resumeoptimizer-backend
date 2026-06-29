package com.vyankatesh.resumeoptimizer.ai.repository;

import com.vyankatesh.resumeoptimizer.ai.entity.AiRecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendationEntity, Long> {

    List<AiRecommendationEntity> findByResumeId(Long resumeId);

    List<AiRecommendationEntity> findByResumeIdOrderByCreatedAtDesc(Long resumeId);
}
