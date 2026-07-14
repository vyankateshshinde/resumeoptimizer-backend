package com.vyankatesh.resumeoptimizer.jobfinder.repository;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobSearchPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobSearchPreferenceRepository
        extends JpaRepository<JobSearchPreferenceEntity, Long> {

    List<JobSearchPreferenceEntity> findByUserEmailOrderByUpdatedAtDesc(String userEmail);

    Optional<JobSearchPreferenceEntity> findByIdAndUserEmail(Long id, String userEmail);
}
