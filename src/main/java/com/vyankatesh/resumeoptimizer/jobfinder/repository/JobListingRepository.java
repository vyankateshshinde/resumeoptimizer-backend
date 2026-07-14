package com.vyankatesh.resumeoptimizer.jobfinder.repository;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface JobListingRepository extends
        JpaRepository<JobListingEntity, Long>,
        JpaSpecificationExecutor<JobListingEntity> {

    Optional<JobListingEntity> findBySourceAndExternalId(
            JobSource source,
            String externalId
    );
}