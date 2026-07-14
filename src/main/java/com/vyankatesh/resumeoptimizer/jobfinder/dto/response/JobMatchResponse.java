package com.vyankatesh.resumeoptimizer.jobfinder.dto.response;

import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSource;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record JobMatchResponse(
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

        BigDecimal minimumSalary,
        BigDecimal maximumSalary,
        String salaryCurrency,
        LocalDateTime postedAt,
        JobSource source,
        String sourceName,
        String applyUrl,
        String descriptionPreview,

        int matchPercentage,
        int resumeMatchPercentage,
        int titleMatchPercentage,
        int experienceMatchPercentage,
        int freshnessPercentage,

        List<String> matchedSkills,
        List<String> missingSkills,
        String matchExplanation
) {

    /**
     * Normalizes nullable response values before JSON serialization.
     */
    public JobMatchResponse {
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

        matchedSkills =
                matchedSkills == null
                        ? List.of()
                        : List.copyOf(matchedSkills);

        missingSkills =
                missingSkills == null
                        ? List.of()
                        : List.copyOf(missingSkills);
    }

    /**
     * Temporary backward-compatible constructor.
     *
     * This allows the existing JobFinderMapper to compile until
     * it is updated in the next step to supply the new fields.
     */
    public JobMatchResponse(
            Long jobId,
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
            LocalDateTime postedAt,
            JobSource source,
            String sourceName,
            String applyUrl,
            String descriptionPreview,
            int matchPercentage,
            int resumeMatchPercentage,
            int titleMatchPercentage,
            int experienceMatchPercentage,
            int freshnessPercentage,
            List<String> matchedSkills,
            List<String> missingSkills,
            String matchExplanation
    ) {
        this(
                jobId,
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
                postedAt,
                source,
                sourceName,
                applyUrl,
                descriptionPreview,
                matchPercentage,
                resumeMatchPercentage,
                titleMatchPercentage,
                experienceMatchPercentage,
                freshnessPercentage,
                matchedSkills,
                missingSkills,
                matchExplanation
        );
    }

    /**
     * Convenient frontend flag for jobs where the extracted
     * experience requirement should be manually verified.
     */
    public boolean experienceVerificationRequired() {
        return experienceRequirementType
                == ExperienceRequirementType.AMBIGUOUS;
    }

    /**
     * Convenient frontend flag indicating whether the requirement
     * is mandatory.
     */
    public boolean strictExperienceRequirement() {
        return experienceRequirementType
                == ExperienceRequirementType.REQUIRED;
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