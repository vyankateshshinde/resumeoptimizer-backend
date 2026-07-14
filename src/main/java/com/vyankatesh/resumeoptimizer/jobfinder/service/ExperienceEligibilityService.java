package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ExperienceEligibilityService {

    public boolean isEligible(
            BigDecimal candidateExperience,
            JobListingEntity job
    ) {
        return evaluate(
                candidateExperience,
                job
        ).eligible();
    }

    public ExperienceEligibility evaluate(
            BigDecimal candidateExperience,
            JobListingEntity job
    ) {
        if (job == null) {
            return new ExperienceEligibility(
                    false,
                    ExperienceEligibilityStatus.INVALID_JOB,
                    "Job information is unavailable"
            );
        }

        if (candidateExperience == null) {
            return new ExperienceEligibility(
                    true,
                    ExperienceEligibilityStatus.CANDIDATE_EXPERIENCE_NOT_PROVIDED,
                    "Candidate experience was not provided"
            );
        }

        if (candidateExperience.compareTo(BigDecimal.ZERO) < 0) {
            return new ExperienceEligibility(
                    false,
                    ExperienceEligibilityStatus.INVALID_CANDIDATE_EXPERIENCE,
                    "Candidate experience cannot be negative"
            );
        }

        ExperienceRequirementType requirementType =
                job.getExperienceRequirementType();

        if (requirementType == null) {
            requirementType =
                    ExperienceRequirementType.NOT_SPECIFIED;
        }

        BigDecimal minimumExperience =
                job.getMinimumExperience();

        BigDecimal maximumExperience =
                job.getMaximumExperience();

        return switch (requirementType) {

            case REQUIRED -> evaluateRequiredExperience(
                    candidateExperience,
                    minimumExperience,
                    maximumExperience
            );

            case PREFERRED -> evaluatePreferredExperience(
                    candidateExperience,
                    minimumExperience,
                    maximumExperience
            );

            case AMBIGUOUS ->
                    new ExperienceEligibility(
                            true,
                            ExperienceEligibilityStatus.VERIFICATION_REQUIRED,
                            buildAmbiguousMessage(
                                    minimumExperience,
                                    maximumExperience
                            )
                    );

            case NOT_SPECIFIED ->
                    new ExperienceEligibility(
                            true,
                            ExperienceEligibilityStatus.NOT_SPECIFIED,
                            "Experience requirement is not specified"
                    );
        };
    }

    private ExperienceEligibility evaluateRequiredExperience(
            BigDecimal candidateExperience,
            BigDecimal minimumExperience,
            BigDecimal maximumExperience
    ) {
        if (minimumExperience == null) {
            return new ExperienceEligibility(
                    true,
                    ExperienceEligibilityStatus.REQUIRED_MINIMUM_UNKNOWN,
                    "Required experience is mentioned, but the minimum value is unknown"
            );
        }

        if (candidateExperience.compareTo(minimumExperience) < 0) {
            return new ExperienceEligibility(
                    false,
                    ExperienceEligibilityStatus.BELOW_REQUIRED_MINIMUM,
                    "Candidate has "
                            + formatYears(candidateExperience)
                            + " years, but the job requires at least "
                            + formatYears(minimumExperience)
                            + " years"
            );
        }

        /*
         * We do not reject candidates who have more experience
         * than the listed maximum. Maximum experience is normally
         * used for ranking or warning, not strict exclusion.
         */
        if (maximumExperience != null
                && candidateExperience.compareTo(maximumExperience) > 0) {

            return new ExperienceEligibility(
                    true,
                    ExperienceEligibilityStatus.ABOVE_LISTED_MAXIMUM,
                    "Candidate meets the minimum requirement but is above the listed range"
            );
        }

        return new ExperienceEligibility(
                true,
                ExperienceEligibilityStatus.ELIGIBLE,
                buildEligibleMessage(
                        minimumExperience,
                        maximumExperience
                )
        );
    }

    private ExperienceEligibility evaluatePreferredExperience(
            BigDecimal candidateExperience,
            BigDecimal minimumExperience,
            BigDecimal maximumExperience
    ) {
        if (minimumExperience == null) {
            return new ExperienceEligibility(
                    true,
                    ExperienceEligibilityStatus.PREFERRED_MINIMUM_UNKNOWN,
                    "Preferred experience is mentioned, but the minimum value is unknown"
            );
        }

        if (candidateExperience.compareTo(minimumExperience) < 0) {
            return new ExperienceEligibility(
                    true,
                    ExperienceEligibilityStatus.BELOW_PREFERRED_MINIMUM,
                    "Candidate is below the preferred experience, but the job remains eligible"
            );
        }

        if (maximumExperience != null
                && candidateExperience.compareTo(maximumExperience) > 0) {

            return new ExperienceEligibility(
                    true,
                    ExperienceEligibilityStatus.ABOVE_LISTED_MAXIMUM,
                    "Candidate meets the preferred minimum but is above the listed range"
            );
        }

        return new ExperienceEligibility(
                true,
                ExperienceEligibilityStatus.PREFERRED_REQUIREMENT_MET,
                "Candidate meets the preferred experience requirement"
        );
    }

    private String buildEligibleMessage(
            BigDecimal minimumExperience,
            BigDecimal maximumExperience
    ) {
        if (maximumExperience != null) {
            return "Candidate fits the required experience range of "
                    + formatYears(minimumExperience)
                    + " to "
                    + formatYears(maximumExperience)
                    + " years";
        }

        return "Candidate meets the minimum required experience of "
                + formatYears(minimumExperience)
                + " years";
    }

    private String buildAmbiguousMessage(
            BigDecimal minimumExperience,
            BigDecimal maximumExperience
    ) {
        if (minimumExperience == null
                && maximumExperience == null) {
            return "Experience requirement is unclear and should be verified in the job description";
        }

        if (maximumExperience != null) {
            return "Possible experience range is "
                    + formatYears(minimumExperience)
                    + " to "
                    + formatYears(maximumExperience)
                    + " years, but verification is required";
        }

        return "Possible minimum experience is "
                + formatYears(minimumExperience)
                + " years, but verification is required";
    }

    private String formatYears(
            BigDecimal value
    ) {
        if (value == null) {
            return "unknown";
        }

        return value.stripTrailingZeros()
                .toPlainString();
    }

    public enum ExperienceEligibilityStatus {

        ELIGIBLE,

        PREFERRED_REQUIREMENT_MET,

        BELOW_REQUIRED_MINIMUM,

        BELOW_PREFERRED_MINIMUM,

        ABOVE_LISTED_MAXIMUM,

        NOT_SPECIFIED,

        VERIFICATION_REQUIRED,

        REQUIRED_MINIMUM_UNKNOWN,

        PREFERRED_MINIMUM_UNKNOWN,

        CANDIDATE_EXPERIENCE_NOT_PROVIDED,

        INVALID_CANDIDATE_EXPERIENCE,

        INVALID_JOB
    }

    public record ExperienceEligibility(
            boolean eligible,
            ExperienceEligibilityStatus status,
            String message
    ) {
    }
}