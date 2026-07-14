package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;
import com.vyankatesh.resumeoptimizer.jobfinder.provider.ExternalJobRecord;
import com.vyankatesh.resumeoptimizer.jobfinder.provider.JobSourceProvider;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobIngestionService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    JobIngestionService.class
            );

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal ONE =
            BigDecimal.ONE;

    private final JobListingRepository jobListingRepository;

    private final List<JobSourceProvider> providers;

    private final HybridExperienceRequirementService
            hybridExperienceRequirementService;

    public JobIngestionService(
            JobListingRepository jobListingRepository,
            List<JobSourceProvider> providers,
            HybridExperienceRequirementService
                    hybridExperienceRequirementService
    ) {
        this.jobListingRepository =
                jobListingRepository;

        this.providers =
                providers;

        this.hybridExperienceRequirementService =
                hybridExperienceRequirementService;
    }

    @Transactional
    public int ingestRecentJobs(
            int lookbackDays
    ) {
        LocalDateTime postedAfter =
                LocalDateTime.now()
                        .minusDays(
                                Math.max(
                                        1,
                                        lookbackDays
                                )
                        );

        int processed = 0;
        int invalidRecords = 0;
        int failedRecords = 0;

        int providerExtractions = 0;
        int ruleExtractions = 0;
        int aiExtractions = 0;
        int ambiguousExtractions = 0;
        int notSpecifiedExtractions = 0;

        for (JobSourceProvider provider : providers) {
            List<ExternalJobRecord> records;

            try {
                records =
                        provider.fetchJobs(
                                postedAfter
                        );

            } catch (Exception exception) {
                log.error(
                        "Job provider {} failed during ingestion: {}",
                        provider.getSource(),
                        exception.getMessage(),
                        exception
                );

                continue;
            }

            if (records == null
                    || records.isEmpty()) {

                log.info(
                        "Job provider {} returned no records",
                        provider.getSource()
                );

                continue;
            }

            for (ExternalJobRecord record : records) {
                if (!isValid(record)) {
                    invalidRecords++;
                    continue;
                }

                try {
                    JobListingEntity entity =
                            jobListingRepository
                                    .findBySourceAndExternalId(
                                            record.source(),
                                            record.externalId()
                                                    .trim()
                                    )
                                    .orElseGet(
                                            JobListingEntity::new
                                    );

                    mapRecord(
                            record,
                            entity
                    );

                    jobListingRepository.save(
                            entity
                    );

                    processed++;

                    String method =
                            normalizeExtractionMethod(
                                    entity.getExperienceExtractionMethod()
                            );

                    if ("PROVIDER".equals(method)) {
                        providerExtractions++;

                    } else if (method.startsWith("AI")
                            || method.contains("_AI")) {
                        aiExtractions++;

                    } else if ("RULE".equals(method)) {
                        ruleExtractions++;
                    }

                    ExperienceRequirementType type =
                            safeRequirementType(
                                    entity.getExperienceRequirementType()
                            );

                    if (type
                            == ExperienceRequirementType.AMBIGUOUS) {
                        ambiguousExtractions++;
                    }

                    if (type
                            == ExperienceRequirementType.NOT_SPECIFIED) {
                        notSpecifiedExtractions++;
                    }

                } catch (Exception exception) {
                    failedRecords++;

                    log.error(
                            "Failed to ingest job '{}' from {}: {}",
                            record.title(),
                            record.source(),
                            exception.getMessage(),
                            exception
                    );
                }
            }
        }

        log.info(
                "Job ingestion completed. Processed: {}, "
                        + "invalid: {}, failed: {}, provider: {}, "
                        + "rule: {}, AI/hybrid: {}, ambiguous: {}, "
                        + "not specified: {}",
                processed,
                invalidRecords,
                failedRecords,
                providerExtractions,
                ruleExtractions,
                aiExtractions,
                ambiguousExtractions,
                notSpecifiedExtractions
        );

        return processed;
    }

    private boolean isValid(
            ExternalJobRecord record
    ) {
        return record != null
                && StringUtils.hasText(
                record.externalId()
        )
                && StringUtils.hasText(
                record.title()
        )
                && StringUtils.hasText(
                record.company()
        )
                && StringUtils.hasText(
                record.description()
        )
                && StringUtils.hasText(
                record.applyUrl()
        )
                && record.source() != null;
    }

    private void mapRecord(
            ExternalJobRecord record,
            JobListingEntity entity
    ) {
        entity.setExternalId(
                record.externalId().trim()
        );

        entity.setTitle(
                record.title().trim()
        );

        entity.setCompany(
                record.company().trim()
        );

        entity.setLocation(
                cleanNullable(
                        record.location()
                )
        );

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

        applyExperienceRequirement(
                record,
                entity
        );

        entity.setMinimumSalary(
                record.minimumSalary()
        );

        entity.setMaximumSalary(
                record.maximumSalary()
        );

        entity.setSalaryCurrency(
                cleanNullable(
                        record.salaryCurrency()
                )
        );

        entity.setDescription(
                record.description().trim()
        );

        entity.setPostedAt(
                record.postedAt() == null
                        ? LocalDateTime.now()
                        : record.postedAt()
        );

        entity.setFetchedAt(
                LocalDateTime.now()
        );

        entity.setSource(
                record.source()
        );

        entity.setSourceName(
                StringUtils.hasText(
                        record.sourceName()
                )
                        ? record.sourceName().trim()
                        : record.source().name()
        );

        entity.setApplyUrl(
                record.applyUrl().trim()
        );

        entity.setActive(true);
    }

    private void applyExperienceRequirement(
            ExternalJobRecord record,
            JobListingEntity entity
    ) {
        /*
         * Provider-supplied structured values take priority.
         * They do not require rule or AI extraction.
         */
        if (record.minimumExperience() != null
                || record.maximumExperience() != null) {

            applyProviderExperience(
                    record,
                    entity
            );

            return;
        }

        try {
            ExperienceRequirementExtractorService
                    .ExperienceRequirement requirement =
                    hybridExperienceRequirementService.extract(
                            record.title(),
                            record.description()
                    );

            applyExtractedRequirement(
                    entity,
                    requirement
            );

        } catch (Exception exception) {
            log.warn(
                    "Hybrid experience extraction failed for "
                            + "job '{}' at '{}': {}",
                    record.title(),
                    record.company(),
                    exception.getMessage()
            );

            applyFailedExtraction(
                    entity
            );
        }
    }

    private void applyProviderExperience(
            ExternalJobRecord record,
            JobListingEntity entity
    ) {
        BigDecimal minimum =
                normalizeExperience(
                        record.minimumExperience()
                );

        BigDecimal maximum =
                normalizeExperience(
                        record.maximumExperience()
                );

        if (minimum != null
                && maximum != null
                && minimum.compareTo(
                maximum
        ) > 0) {

            BigDecimal temporary =
                    minimum;

            minimum =
                    maximum;

            maximum =
                    temporary;
        }

        entity.setMinimumExperience(
                minimum
        );

        entity.setMaximumExperience(
                maximum
        );

        entity.setExperienceRequirementType(
                ExperienceRequirementType.REQUIRED
        );

        entity.setExperienceEvidence(null);

        entity.setExperienceConfidence(
                ONE
        );

        entity.setExperienceExtractionMethod(
                "PROVIDER"
        );
    }

    private void applyExtractedRequirement(
            JobListingEntity entity,
            ExperienceRequirementExtractorService
                    .ExperienceRequirement requirement
    ) {
        if (requirement == null) {
            applyNotSpecified(
                    entity,
                    "RULE"
            );

            return;
        }

        BigDecimal minimum =
                normalizeExperience(
                        requirement.minimumYears()
                );

        BigDecimal maximum =
                normalizeExperience(
                        requirement.maximumYears()
                );

        if (minimum != null
                && maximum != null
                && minimum.compareTo(
                maximum
        ) > 0) {

            BigDecimal temporary =
                    minimum;

            minimum =
                    maximum;

            maximum =
                    temporary;
        }

        ExperienceRequirementType requirementType =
                safeRequirementType(
                        requirement.requirementType()
                );

        /*
         * A specified required or preferred requirement
         * must have a minimum value. Otherwise it is not
         * safe to use for strict eligibility filtering.
         */
        if (
                (
                        requirementType
                                == ExperienceRequirementType.REQUIRED
                                || requirementType
                                == ExperienceRequirementType.PREFERRED
                )
                        && minimum == null
        ) {
            requirementType =
                    ExperienceRequirementType.AMBIGUOUS;
        }

        if (requirementType
                == ExperienceRequirementType.NOT_SPECIFIED) {

            minimum = null;
            maximum = null;
        }

        entity.setMinimumExperience(
                minimum
        );

        entity.setMaximumExperience(
                maximum
        );

        entity.setExperienceRequirementType(
                requirementType
        );

        entity.setExperienceEvidence(
                limitEvidence(
                        requirement.evidence()
                )
        );

        entity.setExperienceConfidence(
                normalizeConfidence(
                        requirement.confidence()
                )
        );

        entity.setExperienceExtractionMethod(
                normalizeExtractionMethod(
                        requirement.extractionMethod()
                )
        );
    }

    private void applyNotSpecified(
            JobListingEntity entity,
            String method
    ) {
        entity.setMinimumExperience(null);
        entity.setMaximumExperience(null);

        entity.setExperienceRequirementType(
                ExperienceRequirementType.NOT_SPECIFIED
        );

        entity.setExperienceEvidence(null);

        entity.setExperienceConfidence(
                ZERO
        );

        entity.setExperienceExtractionMethod(
                normalizeExtractionMethod(
                        method
                )
        );
    }

    private void applyFailedExtraction(
            JobListingEntity entity
    ) {
        entity.setMinimumExperience(null);
        entity.setMaximumExperience(null);

        entity.setExperienceRequirementType(
                ExperienceRequirementType.AMBIGUOUS
        );

        entity.setExperienceEvidence(null);

        entity.setExperienceConfidence(
                ZERO
        );

        entity.setExperienceExtractionMethod(
                "FAILED"
        );
    }

    private ExperienceRequirementType
    safeRequirementType(
            ExperienceRequirementType type
    ) {
        return type == null
                ? ExperienceRequirementType.NOT_SPECIFIED
                : type;
    }

    private BigDecimal normalizeExperience(
            BigDecimal experience
    ) {
        if (experience == null) {
            return null;
        }

        if (experience.compareTo(
                ZERO
        ) < 0) {
            return null;
        }

        if (experience.compareTo(
                new BigDecimal("40")
        ) > 0) {
            return null;
        }

        return experience;
    }

    private BigDecimal normalizeConfidence(
            BigDecimal confidence
    ) {
        if (confidence == null) {
            return ZERO;
        }

        if (confidence.compareTo(
                ZERO
        ) < 0) {
            return ZERO;
        }

        if (confidence.compareTo(
                ONE
        ) > 0) {
            return ONE;
        }

        return confidence;
    }

    private String normalizeExtractionMethod(
            String method
    ) {
        if (!StringUtils.hasText(method)) {
            return "NOT_PROCESSED";
        }

        String normalized =
                method.trim()
                        .toUpperCase();

        return normalized.length() <= 30
                ? normalized
                : normalized.substring(
                0,
                30
        );
    }

    private String limitEvidence(
            String evidence
    ) {
        String cleaned =
                cleanNullable(
                        evidence
                );

        if (cleaned == null) {
            return null;
        }

        return cleaned.length() <= 1000
                ? cleaned
                : cleaned.substring(
                0,
                1000
        );
    }

    private String cleanNullable(
            String value
    ) {
        return StringUtils.hasText(value)
                ? value.trim()
                : null;
    }
}