package com.vyankatesh.resumeoptimizer.jobfinder.repository;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.SavedJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJobEntity, Long> {

    List<SavedJobEntity> findByUserEmailOrderBySavedAtDesc(String userEmail);

    Optional<SavedJobEntity> findByUserEmailAndJobListingId(
            String userEmail,
            Long jobListingId
    );

    boolean existsByUserEmailAndJobListingId(String userEmail, Long jobListingId);
}
