package com.vyankatesh.resumeoptimizer.jobfinder.controller;

import com.vyankatesh.resumeoptimizer.jobfinder.dto.request.JobAlertRequest;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.request.JobFinderSearchRequest;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.request.JobSearchPreferenceRequest;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobAlertResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobDetailsResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobFinderSearchResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobNotificationResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobSearchPreferenceResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.SavedJobResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.service.JobAlertService;
import com.vyankatesh.resumeoptimizer.jobfinder.service.JobFinderService;
import com.vyankatesh.resumeoptimizer.jobfinder.service.JobPreferenceService;
import com.vyankatesh.resumeoptimizer.jobfinder.service.SavedJobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/job-finder")
public class JobFinderController {

    private final JobFinderService jobFinderService;
    private final JobPreferenceService preferenceService;
    private final SavedJobService savedJobService;
    private final JobAlertService alertService;

    public JobFinderController(
            JobFinderService jobFinderService,
            JobPreferenceService preferenceService,
            SavedJobService savedJobService,
            JobAlertService alertService
    ) {
        this.jobFinderService = jobFinderService;
        this.preferenceService = preferenceService;
        this.savedJobService = savedJobService;
        this.alertService = alertService;
    }

    @PostMapping("/search")
    public JobFinderSearchResponse search(
            @Valid @RequestBody JobFinderSearchRequest request,
            Authentication authentication
    ) {
        return jobFinderService.search(request, authentication.getName());
    }

    @GetMapping("/jobs/{jobId}")
    public JobDetailsResponse getJobDetails(@PathVariable Long jobId) {
        return jobFinderService.getJobDetails(jobId);
    }

    @PostMapping("/preferences")
    public JobSearchPreferenceResponse createPreference(
            @Valid @RequestBody JobSearchPreferenceRequest request,
            Authentication authentication
    ) {
        return preferenceService.create(request, authentication.getName());
    }

    @PutMapping("/preferences/{preferenceId}")
    public JobSearchPreferenceResponse updatePreference(
            @PathVariable Long preferenceId,
            @Valid @RequestBody JobSearchPreferenceRequest request,
            Authentication authentication
    ) {
        return preferenceService.update(
                preferenceId,
                request,
                authentication.getName()
        );
    }

    @GetMapping("/preferences")
    public List<JobSearchPreferenceResponse> listPreferences(
            Authentication authentication
    ) {
        return preferenceService.list(authentication.getName());
    }

    @DeleteMapping("/preferences/{preferenceId}")
    public ResponseEntity<Void> deletePreference(
            @PathVariable Long preferenceId,
            Authentication authentication
    ) {
        preferenceService.delete(preferenceId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/saved-jobs/{jobId}")
    public SavedJobResponse saveJob(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        return savedJobService.save(jobId, authentication.getName());
    }

    @GetMapping("/saved-jobs")
    public List<SavedJobResponse> listSavedJobs(Authentication authentication) {
        return savedJobService.list(authentication.getName());
    }

    @DeleteMapping("/saved-jobs/{jobId}")
    public ResponseEntity<Void> removeSavedJob(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        savedJobService.remove(jobId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alerts")
    public JobAlertResponse createOrUpdateAlert(
            @Valid @RequestBody JobAlertRequest request,
            Authentication authentication
    ) {
        return alertService.createOrUpdate(request, authentication.getName());
    }

    @GetMapping("/alerts")
    public List<JobAlertResponse> listAlerts(Authentication authentication) {
        return alertService.list(authentication.getName());
    }

    @PatchMapping("/alerts/{alertId}/enabled")
    public JobAlertResponse setAlertEnabled(
            @PathVariable Long alertId,
            @RequestParam boolean enabled,
            Authentication authentication
    ) {
        return alertService.setEnabled(
                alertId,
                enabled,
                authentication.getName()
        );
    }

    @DeleteMapping("/alerts/{alertId}")
    public ResponseEntity<Void> deleteAlert(
            @PathVariable Long alertId,
            Authentication authentication
    ) {
        alertService.delete(alertId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notifications")
    public List<JobNotificationResponse> listNotifications(
            Authentication authentication
    ) {
        return alertService.listNotifications(authentication.getName());
    }

    @GetMapping("/notifications/unread-count")
    public Map<String, Long> countUnreadNotifications(
            Authentication authentication
    ) {
        return Map.of(
                "unreadCount",
                alertService.countUnreadNotifications(authentication.getName())
        );
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public JobNotificationResponse markNotificationRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {
        return alertService.markNotificationRead(
                notificationId,
                authentication.getName()
        );
    }
}
