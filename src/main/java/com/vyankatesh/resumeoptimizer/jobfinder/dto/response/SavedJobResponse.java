package com.vyankatesh.resumeoptimizer.jobfinder.dto.response;

import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SavedJobResponse(
        Long savedJobId,
        Long jobId,
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

        LocalDateTime postedAt,
        String applyUrl,
        LocalDateTime savedAt
) {

    public SavedJobResponse {
        experienceRequirementType =
                experienceRequirementType == null
                        ? ExperienceRequirementType.NOT_SPECIFIED
                        : experienceRequirementType;

        experienceConfidence =
                normalizeConfidence(
                        experienceConfidence
                );

        experienceEvidence =
                cleanNullable(
                        experienceEvidence
                );

        experienceExtractionMethod =
                cleanNullable(
                        experienceExtractionMethod
                );

        if (experienceExtractionMethod == null) {
            experienceExtractionMethod =
                    "NOT_PROCESSED";
        }
    }

    /**
     * Backward-compatible constructor.
     *
     * This keeps the existing JobFinderMapper compiling until
     * it is updated in the next step.
     */
    public SavedJobResponse(
            Long savedJobId,
            Long jobId,
            String title,
            String company,
            String location,
            WorkArrangement workArrangement,
            EmploymentType employmentType,
            LocalDateTime postedAt,
            String applyUrl,
            LocalDateTime savedAt
    ) {
        this(
                savedJobId,
                jobId,
                title,
                company,
                location,
                workArrangement,
                employmentType,

                null,
                null,
                ExperienceRequirementType.NOT_SPECIFIED,
                null,
                BigDecimal.ZERO,
                "NOT_PROCESSED",

                postedAt,
                applyUrl,
                savedAt
        );
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

    private static String cleanNullable(
            String value
    ) {
        if (value == null
                || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}