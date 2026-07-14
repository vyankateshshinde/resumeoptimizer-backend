package com.vyankatesh.resumeoptimizer.jobfinder.dto.response;

import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSource;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record JobDetailsResponse(
        Long jobId,
        String externalId,
        String title,
        String company,
        String location,
        WorkArrangement workArrangement,
        EmploymentType employmentType,

        BigDecimal minimumExperience,
        BigDecimal maximumExperience,
        ExperienceRequirementType experienceRequirementType,
        String experienceEvidence,
        BigDecimal experienceConfidence,
        String experienceExtractionMethod,

        BigDecimal minimumSalary,
        BigDecimal maximumSalary,
        String salaryCurrency,
        String description,
        LocalDateTime postedAt,
        LocalDateTime fetchedAt,
        JobSource source,
        String sourceName,
        String applyUrl
) {

    public JobDetailsResponse {
        experienceRequirementType =
                experienceRequirementType == null
                        ? ExperienceRequirementType.NOT_SPECIFIED
                        : experienceRequirementType;

        experienceConfidence =
                normalizeConfidence(
                        experienceConfidence
                );

        experienceExtractionMethod =
                experienceExtractionMethod == null
                        || experienceExtractionMethod.isBlank()
                        ? "NOT_PROCESSED"
                        : experienceExtractionMethod.trim();

        experienceEvidence =
                experienceEvidence == null
                        || experienceEvidence.isBlank()
                        ? null
                        : experienceEvidence.trim();
    }

    /**
     * Temporary backward-compatible constructor.
     *
     * This allows the current JobFinderMapper to compile until
     * the details mapping is updated in the next step.
     */
    public JobDetailsResponse(
            Long jobId,
            String externalId,
            String title,
            String company,
            String location,
            WorkArrangement workArrangement,
            EmploymentType employmentType,
            BigDecimal minimumExperience,
            BigDecimal maximumExperience,
            BigDecimal minimumSalary,
            BigDecimal maximumSalary,
            String salaryCurrency,
            String description,
            LocalDateTime postedAt,
            LocalDateTime fetchedAt,
            JobSource source,
            String sourceName,
            String applyUrl
    ) {
        this(
                jobId,
                externalId,
                title,
                company,
                location,
                workArrangement,
                employmentType,
                minimumExperience,
                maximumExperience,
                ExperienceRequirementType.NOT_SPECIFIED,
                null,
                BigDecimal.ZERO,
                "NOT_PROCESSED",
                minimumSalary,
                maximumSalary,
                salaryCurrency,
                description,
                postedAt,
                fetchedAt,
                source,
                sourceName,
                applyUrl
        );
    }

    public boolean experienceVerificationRequired() {
        return experienceRequirementType
                == ExperienceRequirementType.AMBIGUOUS;
    }

    public boolean strictExperienceRequirement() {
        return experienceRequirementType
                == ExperienceRequirementType.REQUIRED;
    }

    public boolean preferredExperienceRequirement() {
        return experienceRequirementType
                == ExperienceRequirementType.PREFERRED;
    }

    public boolean experienceSpecified() {
        return minimumExperience != null
                || maximumExperience != null;
    }

    private static BigDecimal normalizeConfidence(
            BigDecimal confidence
    ) {
        if (confidence == null) {
            return BigDecimal.ZERO;
        }

        if (confidence.compareTo(
                BigDecimal.ZERO
        ) < 0) {
            return BigDecimal.ZERO;
        }

        if (confidence.compareTo(
                BigDecimal.ONE
        ) > 0) {
            return BigDecimal.ONE;
        }

        return confidence;
    }
}