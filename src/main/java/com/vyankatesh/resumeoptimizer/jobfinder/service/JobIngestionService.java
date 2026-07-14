package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;
import com.vyankatesh.resumeoptimizer.jobfinder.provider.ExternalJobRecord;
import com.vyankatesh.resumeoptimizer.jobfinder.provider.JobSourceProvider;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobIngestionService {

    private final JobListingRepository jobListingRepository;
    private final List<JobSourceProvider> providers;

    public JobIngestionService(
            JobListingRepository jobListingRepository,
            List<JobSourceProvider> providers
    ) {
        this.jobListingRepository = jobListingRepository;
        this.providers = providers;
    }

    @Transactional
    public int ingestRecentJobs(int lookbackDays) {
        LocalDateTime postedAfter = LocalDateTime.now().minusDays(Math.max(1, lookbackDays));
        int processed = 0;

        for (JobSourceProvider provider : providers) {
            List<ExternalJobRecord> records = provider.fetchJobs(postedAfter);

            for (ExternalJobRecord record : records) {
                if (!isValid(record)) {
                    continue;
                }

                JobListingEntity entity = jobListingRepository
                        .findBySourceAndExternalId(record.source(), record.externalId())
                        .orElseGet(JobListingEntity::new);

                mapRecord(record, entity);
                jobListingRepository.save(entity);
                processed++;
            }
        }

        return processed;
    }

    private boolean isValid(ExternalJobRecord record) {
        return record != null
                && hasText(record.externalId())
                && hasText(record.title())
                && hasText(record.company())
                && hasText(record.description())
                && hasText(record.applyUrl())
                && record.source() != null;
    }

    private void mapRecord(ExternalJobRecord record, JobListingEntity entity) {
        entity.setExternalId(record.externalId().trim());
        entity.setTitle(record.title().trim());
        entity.setCompany(record.company().trim());
        entity.setLocation(cleanNullable(record.location()));
        entity.setWorkArrangement(
                record.workArrangement() == null
                        ? WorkArrangement.UNSPECIFIED
                        : record.workArrangement()
        );
        entity.setEmploymentType(
                record.employmentType() == null
                        ? EmploymentType.OTHER
                        : record.employmentType()
        );
        entity.setMinimumExperience(record.minimumExperience());
        entity.setMaximumExperience(record.maximumExperience());
        entity.setMinimumSalary(record.minimumSalary());
        entity.setMaximumSalary(record.maximumSalary());
        entity.setSalaryCurrency(cleanNullable(record.salaryCurrency()));
        entity.setDescription(record.description().trim());
        entity.setPostedAt(
                record.postedAt() == null ? LocalDateTime.now() : record.postedAt()
        );
        entity.setFetchedAt(LocalDateTime.now());
        entity.setSource(record.source());
        entity.setSourceName(
                hasText(record.sourceName())
                        ? record.sourceName().trim()
                        : record.source().name()
        );
        entity.setApplyUrl(record.applyUrl().trim());
        entity.setActive(true);
    }

    private String cleanNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
