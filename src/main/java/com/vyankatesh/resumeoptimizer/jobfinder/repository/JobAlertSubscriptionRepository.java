package com.vyankatesh.resumeoptimizer.jobfinder.repository;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobAlertSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobAlertSubscriptionRepository
        extends JpaRepository<JobAlertSubscriptionEntity, Long> {

    List<JobAlertSubscriptionEntity> findByUserEmailOrderByUpdatedAtDesc(String userEmail);

    Optional<JobAlertSubscriptionEntity> findByIdAndUserEmail(Long id, String userEmail);

    Optional<JobAlertSubscriptionEntity> findByPreferenceIdAndUserEmail(
            Long preferenceId,
            String userEmail
    );

    List<JobAlertSubscriptionEntity> findTop500ByEnabledTrueOrderByLastCheckedAtAsc();
}
