package com.vyankatesh.resumeoptimizer.jobfinder.repository;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobNotificationRepository
        extends JpaRepository<JobNotificationEntity, Long> {

    List<JobNotificationEntity> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Optional<JobNotificationEntity> findByIdAndUserEmail(Long id, String userEmail);

    boolean existsByAlertSubscriptionIdAndJobListingId(
            Long alertSubscriptionId,
            Long jobListingId
    );

    long countByUserEmailAndReadFalse(String userEmail);

    void deleteByAlertSubscriptionId(Long alertSubscriptionId);
}
