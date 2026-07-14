package com.vyankatesh.resumeoptimizer.jobfinder.mapper;

import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobAlertResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobDetailsResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobMatchResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobNotificationResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobSearchPreferenceResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.SavedJobResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobAlertSubscriptionEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobNotificationEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobSearchPreferenceEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.entity.SavedJobEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobMatchResult;
import org.springframework.stereotype.Component;

@Component
public class JobFinderMapper {

    public JobMatchResponse toMatchResponse(
            JobListingEntity job,
            JobMatchResult match
    ) {
        return new JobMatchResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getWorkArrangement(),
                job.getEmploymentType(),
                job.getMinimumExperience(),
                job.getMaximumExperience(),
                job.getMinimumSalary(),
                job.getMaximumSalary(),
                job.getSalaryCurrency(),
                job.getPostedAt(),
                job.getSource(),
                job.getSourceName(),
                job.getApplyUrl(),
                preview(job.getDescription(), 280),
                match.overallScore(),
                match.resumeScore(),
                match.titleScore(),
                match.experienceScore(),
                match.freshnessScore(),
                match.matchedSkills(),
                match.missingSkills(),
                match.explanation()
        );
    }

    public JobDetailsResponse toDetailsResponse(JobListingEntity job) {
        return new JobDetailsResponse(
                job.getId(),
                job.getExternalId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getWorkArrangement(),
                job.getEmploymentType(),
                job.getMinimumExperience(),
                job.getMaximumExperience(),
                job.getMinimumSalary(),
                job.getMaximumSalary(),
                job.getSalaryCurrency(),
                job.getDescription(),
                job.getPostedAt(),
                job.getFetchedAt(),
                job.getSource(),
                job.getSourceName(),
                job.getApplyUrl()
        );
    }

    public JobSearchPreferenceResponse toPreferenceResponse(
            JobSearchPreferenceEntity preference
    ) {
        return new JobSearchPreferenceResponse(
                preference.getId(),
                preference.getName(),
                preference.getResumeId(),
                ListCopy.of(preference.getJobTitles()),
                ListCopy.of(preference.getLocations()),
                SetCopy.of(preference.getWorkArrangements()),
                SetCopy.of(preference.getEmploymentTypes()),
                preference.getExperienceYears(),
                preference.getPostedWithinDays(),
                preference.getMinimumSalary(),
                preference.getMinimumMatchPercentage(),
                preference.getSortBy(),
                preference.getCreatedAt(),
                preference.getUpdatedAt()
        );
    }

    public SavedJobResponse toSavedJobResponse(SavedJobEntity savedJob) {
        JobListingEntity job = savedJob.getJobListing();

        return new SavedJobResponse(
                savedJob.getId(),
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getWorkArrangement(),
                job.getEmploymentType(),
                job.getPostedAt(),
                job.getApplyUrl(),
                savedJob.getSavedAt()
        );
    }

    public JobAlertResponse toAlertResponse(JobAlertSubscriptionEntity alert) {
        return new JobAlertResponse(
                alert.getId(),
                toPreferenceResponse(alert.getPreference()),
                alert.isEnabled(),
                alert.getLastCheckedAt(),
                alert.getCreatedAt(),
                alert.getUpdatedAt()
        );
    }

    public JobNotificationResponse toNotificationResponse(
            JobNotificationEntity notification
    ) {
        JobListingEntity job = notification.getJobListing();

        return new JobNotificationResponse(
                notification.getId(),
                notification.getAlertSubscription().getId(),
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getApplyUrl(),
                notification.getMatchPercentage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    private String preview(String value, int maximumLength) {
        if (value == null) {
            return "";
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maximumLength
                ? normalized
                : normalized.substring(0, maximumLength).trim() + "...";
    }

    private static final class ListCopy {
        private ListCopy() {
        }

        private static <T> java.util.List<T> of(java.util.Collection<T> values) {
            return values == null ? java.util.List.of() : java.util.List.copyOf(values);
        }
    }

    private static final class SetCopy {
        private SetCopy() {
        }

        private static <T> java.util.Set<T> of(java.util.Collection<T> values) {
            return values == null ? java.util.Set.of() : java.util.Set.copyOf(values);
        }
    }
}
