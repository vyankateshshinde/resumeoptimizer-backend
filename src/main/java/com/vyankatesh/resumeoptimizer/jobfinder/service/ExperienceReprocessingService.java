package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSource;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ExperienceReprocessingService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ExperienceReprocessingService.class
            );

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal ONE =
            BigDecimal.ONE;

    private final JobListingRepository jobListingRepository;

    private final HybridExperienceRequirementService
            hybridExperienceRequirementService;

    public ExperienceReprocessingService(
            JobListingRepository jobListingRepository,
            HybridExperienceRequirementService
                    hybridExperienceRequirementService
    ) {
        this.jobListingRepository =
                jobListingRepository;

        this.hybridExperienceRequirementService =
                hybridExperienceRequirementService;
    }

    @Transactional
    public ReprocessingResult reprocessAllJobs() {
        List<JobListingEntity> jobs =
                jobListingRepository
                        .findAllByActiveTrue();

        return reprocessJobs(
                jobs,
                "ALL"
        );
    }

    @Transactional
    public ReprocessingResult reprocessJobsBySource(
            JobSource source
    ) {
        if (source == null) {
            return reprocessAllJobs();
        }

        List<JobListingEntity> jobs =
                jobListingRepository
                        .findAllByActiveTrueAndSource(
                                source
                        );

        return reprocessJobs(
                jobs,
                source.name()
        );
    }

    private ReprocessingResult reprocessJobs(
            List<JobListingEntity> jobs,
            String sourceName
    ) {
        int scanned = 0;
        int updated = 0;

        int required = 0;
        int preferred = 0;
        int ambiguous = 0;
        int notSpecified = 0;

        int providerPreserved = 0;
        int skipped = 0;
        int failed = 0;

        List<JobListingEntity> successfullyUpdatedJobs =
                new ArrayList<>();

        for (JobListingEntity job : jobs) {
            scanned++;

            if (!StringUtils.hasText(
                    job.getTitle()
            )
                    || !StringUtils.hasText(
                    job.getDescription()
            )) {

                skipped++;

                log.warn(
                        "Skipping experience reprocessing "
                                + "for job ID {} because title "
                                + "or description is missing",
                        job.getId()
                );

                continue;
            }

            /*
             * Structured provider experience has the highest
             * priority and should not be overwritten by AI.
             */
            if (hasStructuredProviderExperience(job)) {
                providerPreserved++;
                continue;
            }

            try {
                ExperienceRequirementExtractorService
                        .ExperienceRequirement requirement =
                        hybridExperienceRequirementService.extract(
                                job.getTitle(),
                                job.getDescription()
                        );

                applyRequirement(
                        job,
                        requirement
                );

                successfullyUpdatedJobs.add(job);
                updated++;

                switch (
                        safeRequirementType(
                                job.getExperienceRequirementType()
                        )
                ) {
                    case REQUIRED ->
                            required++;

                    case PREFERRED ->
                            preferred++;

                    case AMBIGUOUS ->
                            ambiguous++;

                    case NOT_SPECIFIED ->
                            notSpecified++;
                }

            } catch (Exception exception) {
                failed++;

                /*
                 * Existing values remain unchanged when
                 * reprocessing fails.
                 */
                log.error(
                        "Experience reprocessing failed for "
                                + "job ID {}, title '{}': {}",
                        job.getId(),
                        job.getTitle(),
                        exception.getMessage(),
                        exception
                );
            }
        }

        if (!successfullyUpdatedJobs.isEmpty()) {
            jobListingRepository.saveAll(
                    successfullyUpdatedJobs
            );

            jobListingRepository.flush();
        }

        ReprocessingResult result =
                new ReprocessingResult(
                        sourceName,
                        scanned,
                        updated,
                        required,
                        preferred,
                        ambiguous,
                        notSpecified,
                        providerPreserved,
                        skipped,
                        failed
                );

        log.info(
                "Experience reprocessing completed: {}",
                result
        );

        return result;
    }

    private boolean hasStructuredProviderExperience(
            JobListingEntity job
    ) {
        String method =
                normalizeMethod(
                        job.getExperienceExtractionMethod()
                );

        boolean hasExperienceValue =
                job.getMinimumExperience() != null
                        || job.getMaximumExperience() != null;

        return "PROVIDER".equals(method)
                && hasExperienceValue;
    }

    private void applyRequirement(
            JobListingEntity job,
            ExperienceRequirementExtractorService
                    .ExperienceRequirement requirement
    ) {
        if (requirement == null) {
            applyNotSpecified(job);
            return;
        }

        BigDecimal minimumExperience =
                normalizeExperience(
                        requirement.minimumYears()
                );

        BigDecimal maximumExperience =
                normalizeExperience(
                        requirement.maximumYears()
                );

        if (minimumExperience != null
                && maximumExperience != null
                && minimumExperience.compareTo(
                maximumExperience
        ) > 0) {

            BigDecimal temporary =
                    minimumExperience;

            minimumExperience =
                    maximumExperience;

            maximumExperience =
                    temporary;
        }

        ExperienceRequirementType requirementType =
                safeRequirementType(
                        requirement.requirementType()
                );

        /*
         * Required and preferred values without a minimum
         * are not safe enough for strict filtering.
         */
        if (
                (
                        requirementType
                                == ExperienceRequirementType.REQUIRED
                                || requirementType
                                == ExperienceRequirementType.PREFERRED
                )
                        && minimumExperience == null
        ) {
            requirementType =
                    ExperienceRequirementType.AMBIGUOUS;
        }

        if (requirementType
                == ExperienceRequirementType.NOT_SPECIFIED) {

            minimumExperience = null;
            maximumExperience = null;
        }

        job.setMinimumExperience(
                minimumExperience
        );

        job.setMaximumExperience(
                maximumExperience
        );

        job.setExperienceRequirementType(
                requirementType
        );

        job.setExperienceEvidence(
                limitEvidence(
                        requirement.evidence()
                )
        );

        job.setExperienceConfidence(
                normalizeConfidence(
                        requirement.confidence()
                )
        );

        job.setExperienceExtractionMethod(
                normalizeMethod(
                        requirement.extractionMethod()
                )
        );
    }

    private void applyNotSpecified(
            JobListingEntity job
    ) {
        job.setMinimumExperience(null);
        job.setMaximumExperience(null);

        job.setExperienceRequirementType(
                ExperienceRequirementType.NOT_SPECIFIED
        );

        job.setExperienceEvidence(null);

        job.setExperienceConfidence(
                ZERO
        );

        job.setExperienceExtractionMethod(
                "RULE"
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

        if (experience.compareTo(ZERO) < 0
                || experience.compareTo(
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

        if (confidence.compareTo(ZERO) < 0) {
            return ZERO;
        }

        if (confidence.compareTo(ONE) > 0) {
            return ONE;
        }

        return confidence;
    }

    private String normalizeMethod(
            String method
    ) {
        if (!StringUtils.hasText(method)) {
            return "NOT_PROCESSED";
        }

        String normalized =
                method.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

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
        if (!StringUtils.hasText(evidence)) {
            return null;
        }

        String cleaned =
                evidence.trim();

        return cleaned.length() <= 1000
                ? cleaned
                : cleaned.substring(
                0,
                1000
        );
    }

    public record ReprocessingResult(
            String source,
            int scanned,
            int updated,
            int required,
            int preferred,
            int ambiguous,
            int notSpecified,
            int providerPreserved,
            int skipped,
            int failed
    ) {
    }
}