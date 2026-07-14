package com.vyankatesh.resumeoptimizer.jobfinder.dto.response;

import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record JobNotificationResponse(
        Long id,
        Long alertId,
        Long jobId,
        String title,
        String company,
        String location,

        BigDecimal minimumExperience,
        BigDecimal maximumExperience,
        ExperienceRequirementType experienceRequirementType,
        String experienceEvidence,
        BigDecimal experienceConfidence,
        String experienceExtractionMethod,

        String applyUrl,
        int matchPercentage,
        boolean read,
        LocalDateTime createdAt
) {

    public JobNotificationResponse {
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
     * Backward-compatible constructor for older mapper calls.
     */
    public JobNotificationResponse(
            Long id,
            Long alertId,
            Long jobId,
            String title,
            String company,
            String location,
            String applyUrl,
            int matchPercentage,
            boolean read,
            LocalDateTime createdAt
    ) {
        this(
                id,
                alertId,
                jobId,
                title,
                company,
                location,

                null,
                null,
                ExperienceRequirementType.NOT_SPECIFIED,
                null,
                BigDecimal.ZERO,
                "NOT_PROCESSED",

                applyUrl,
                matchPercentage,
                read,
                createdAt
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