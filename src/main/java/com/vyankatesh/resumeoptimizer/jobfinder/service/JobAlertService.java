package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.dto.request.JobAlertRequest;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobAlertResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobNotificationResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobAlertSubscriptionEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobNotificationEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobSearchPreferenceEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.mapper.JobFinderMapper;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSearchCriteria;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobAlertSubscriptionRepository;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobNotificationRepository;
import com.vyankatesh.resumeoptimizer.resume.ResumeEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobAlertService {

    private final JobAlertSubscriptionRepository alertRepository;
    private final JobNotificationRepository notificationRepository;
    private final JobPreferenceService preferenceService;
    private final JobFinderService jobFinderService;
    private final JobFinderMapper mapper;

    public JobAlertService(
            JobAlertSubscriptionRepository alertRepository,
            JobNotificationRepository notificationRepository,
            JobPreferenceService preferenceService,
            JobFinderService jobFinderService,
            JobFinderMapper mapper
    ) {
        this.alertRepository = alertRepository;
        this.notificationRepository = notificationRepository;
        this.preferenceService = preferenceService;
        this.jobFinderService = jobFinderService;
        this.mapper = mapper;
    }

    @Transactional
    public JobAlertResponse createOrUpdate(
            JobAlertRequest request,
            String userEmail
    ) {
        JobSearchPreferenceEntity preference = preferenceService.getOwnedPreference(
                request.preferenceId(),
                userEmail
        );

        JobAlertSubscriptionEntity alert = alertRepository
                .findByPreferenceIdAndUserEmail(preference.getId(), userEmail)
                .orElseGet(JobAlertSubscriptionEntity::new);

        alert.setUserEmail(userEmail);
        alert.setPreference(preference);
        alert.setEnabled(request.enabled() == null || request.enabled());

        return mapper.toAlertResponse(alertRepository.save(alert));
    }

    public List<JobAlertResponse> list(String userEmail) {
        return alertRepository.findByUserEmailOrderByUpdatedAtDesc(userEmail)
                .stream()
                .map(mapper::toAlertResponse)
                .toList();
    }

    @Transactional
    public JobAlertResponse setEnabled(
            Long alertId,
            boolean enabled,
            String userEmail
    ) {
        JobAlertSubscriptionEntity alert = getOwnedAlert(alertId, userEmail);
        alert.setEnabled(enabled);
        return mapper.toAlertResponse(alertRepository.save(alert));
    }

    @Transactional
    public void delete(Long alertId, String userEmail) {
        JobAlertSubscriptionEntity alert = getOwnedAlert(alertId, userEmail);
        notificationRepository.deleteByAlertSubscriptionId(alert.getId());
        alertRepository.delete(alert);
    }

    public List<JobNotificationResponse> listNotifications(String userEmail) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(mapper::toNotificationResponse)
                .toList();
    }

    public long countUnreadNotifications(String userEmail) {
        return notificationRepository.countByUserEmailAndReadFalse(userEmail);
    }

    @Transactional
    public JobNotificationResponse markNotificationRead(
            Long notificationId,
            String userEmail
    ) {
        JobNotificationEntity notification = notificationRepository
                .findByIdAndUserEmail(notificationId, userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job notification not found"
                ));

        notification.setRead(true);
        return mapper.toNotificationResponse(notificationRepository.save(notification));
    }

    @Transactional
    public int processEnabledAlerts() {
        List<JobAlertSubscriptionEntity> alerts = alertRepository
                .findTop500ByEnabledTrueOrderByLastCheckedAtAsc();

        int createdNotifications = 0;

        for (JobAlertSubscriptionEntity alert : alerts) {
            createdNotifications += processAlert(alert);
        }

        return createdNotifications;
    }

    private int processAlert(JobAlertSubscriptionEntity alert) {
        JobSearchPreferenceEntity preference = alert.getPreference();
        ResumeEntity resume;

        try {
            resume = jobFinderService.getOwnedResume(
                    preference.getResumeId(),
                    alert.getUserEmail()
            );
        } catch (ResponseStatusException exception) {
            alert.setEnabled(false);
            alertRepository.save(alert);
            return 0;
        }

        LocalDateTime fallbackPostedAfter = LocalDateTime.now()
                .minusDays(preference.getPostedWithinDays());
        LocalDateTime postedAfter = alert.getLastCheckedAt() == null
                ? fallbackPostedAfter
                : alert.getLastCheckedAt().minusMinutes(5);

        JobSearchCriteria criteria = new JobSearchCriteria(
                List.copyOf(preference.getJobTitles()),
                List.copyOf(preference.getLocations()),
                SetCopy.of(preference.getWorkArrangements()),
                SetCopy.of(preference.getEmploymentTypes()),
                preference.getExperienceYears(),
                preference.getMinimumSalary(),
                postedAfter,
                preference.getSortBy(),
                preference.getMinimumMatchPercentage()
        );

        List<JobFinderService.ScoredJob> matches = jobFinderService.scoreCandidates(
                resume.getExtractedText(),
                criteria,
                200
        );

        int created = 0;

        for (JobFinderService.ScoredJob scoredJob : matches) {
            boolean alreadyExists = notificationRepository
                    .existsByAlertSubscriptionIdAndJobListingId(
                            alert.getId(),
                            scoredJob.job().getId()
                    );

            if (alreadyExists) {
                continue;
            }

            JobNotificationEntity notification = new JobNotificationEntity();
            notification.setUserEmail(alert.getUserEmail());
            notification.setAlertSubscription(alert);
            notification.setJobListing(scoredJob.job());
            notification.setMatchPercentage(scoredJob.match().overallScore());
            notification.setRead(false);
            notificationRepository.save(notification);
            created++;
        }

        alert.setLastCheckedAt(LocalDateTime.now());
        alertRepository.save(alert);

        return created;
    }

    private JobAlertSubscriptionEntity getOwnedAlert(
            Long alertId,
            String userEmail
    ) {
        return alertRepository.findByIdAndUserEmail(alertId, userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job alert not found"
                ));
    }

    private static final class SetCopy {
        private SetCopy() {
        }

        private static <T> java.util.Set<T> of(java.util.Collection<T> values) {
            return values == null ? java.util.Set.of() : java.util.Set.copyOf(values);
        }
    }
}
