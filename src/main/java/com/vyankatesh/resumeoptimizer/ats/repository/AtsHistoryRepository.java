package com.vyankatesh.resumeoptimizer.ats.repository;

import com.vyankatesh.resumeoptimizer.ats.entity.AtsHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AtsHistoryRepository extends JpaRepository<AtsHistoryEntity, Long> {

    List<AtsHistoryEntity> findByEmailOrderByCreatedAtDesc(String email);

    Optional<AtsHistoryEntity> findTopByEmailAndResumeIdOrderByCreatedAtDesc(
            String email,
            Long resumeId
    );

    Optional<AtsHistoryEntity> findTopByEmailAndResumeIdAndJobDescriptionOrderByCreatedAtDesc(
            String email,
            Long resumeId,
            String jobDescription
    );

    long countByEmail(String email);

    @Query("""
            SELECT MAX(a.finalScore)
            FROM AtsHistoryEntity a
            WHERE a.email = :email
            """)
    Integer getHighestScore(@Param("email") String email);

    @Query("""
            SELECT AVG(a.finalScore)
            FROM AtsHistoryEntity a
            WHERE a.email = :email
            """)
    Double getAverageScore(@Param("email") String email);
}
