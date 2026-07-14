package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.dto.request.JobSearchPreferenceRequest;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobSearchPreferenceResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobSearchPreferenceEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.mapper.JobFinderMapper;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSortOption;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobAlertSubscriptionRepository;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobNotificationRepository;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobSearchPreferenceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class JobPreferenceService {

    private final JobSearchPreferenceRepository preferenceRepository;
    private final JobFinderService jobFinderService;
    private final JobAlertSubscriptionRepository alertRepository;
    private final JobNotificationRepository notificationRepository;
    private final JobFinderMapper mapper;

    public JobPreferenceService(
            JobSearchPreferenceRepository preferenceRepository,
            JobFinderService jobFinderService,
            JobAlertSubscriptionRepository alertRepository,
            JobNotificationRepository notificationRepository,
            JobFinderMapper mapper
    ) {
        this.preferenceRepository = preferenceRepository;
        this.jobFinderService = jobFinderService;
        this.alertRepository = alertRepository;
        this.notificationRepository = notificationRepository;
        this.mapper = mapper;
    }

    @Transactional
    public JobSearchPreferenceResponse create(
            JobSearchPreferenceRequest request,
            String userEmail
    ) {
        jobFinderService.getOwnedResume(request.resumeId(), userEmail);

        JobSearchPreferenceEntity preference = new JobSearchPreferenceEntity();
        preference.setUserEmail(userEmail);
        applyRequest(preference, request);

        return mapper.toPreferenceResponse(preferenceRepository.save(preference));
    }

    @Transactional
    public JobSearchPreferenceResponse update(
            Long preferenceId,
            JobSearchPreferenceRequest request,
            String userEmail
    ) {
        jobFinderService.getOwnedResume(request.resumeId(), userEmail);

        JobSearchPreferenceEntity preference = getOwnedPreference(
                preferenceId,
                userEmail
        );
        applyRequest(preference, request);

        return mapper.toPreferenceResponse(preferenceRepository.save(preference));
    }

    public List<JobSearchPreferenceResponse> list(String userEmail) {
        return preferenceRepository
                .findByUserEmailOrderByUpdatedAtDesc(userEmail)
                .stream()
                .map(mapper::toPreferenceResponse)
                .toList();
    }

    @Transactional
    public void delete(Long preferenceId, String userEmail) {
        JobSearchPreferenceEntity preference = getOwnedPreference(
                preferenceId,
                userEmail
        );

        alertRepository.findByPreferenceIdAndUserEmail(preferenceId, userEmail)
                .ifPresent(alert -> {
                    notificationRepository.deleteByAlertSubscriptionId(alert.getId());
                    alertRepository.delete(alert);
                });

        preferenceRepository.delete(preference);
    }

    public JobSearchPreferenceEntity getOwnedPreference(
            Long preferenceId,
            String userEmail
    ) {
        return preferenceRepository.findByIdAndUserEmail(preferenceId, userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job search preference not found"
                ));
    }

    private void applyRequest(
            JobSearchPreferenceEntity preference,
            JobSearchPreferenceRequest request
    ) {
        preference.setName(request.name().trim());
        preference.setResumeId(request.resumeId());
        preference.setJobTitles(request.jobTitles());
        preference.setLocations(request.locations());
        preference.setWorkArrangements(request.workArrangements());
        preference.setEmploymentTypes(request.employmentTypes());
        preference.setExperienceYears(request.experienceYears());
        preference.setPostedWithinDays(
                request.postedWithinDays() == null ? 7 : request.postedWithinDays()
        );
        preference.setMinimumSalary(request.minimumSalary());
        preference.setMinimumMatchPercentage(
                request.minimumMatchPercentage() == null
                        ? 60
                        : request.minimumMatchPercentage()
        );
        preference.setSortBy(
                request.sortBy() == null
                        ? JobSortOption.BEST_MATCH
                        : request.sortBy()
        );
    }
}
