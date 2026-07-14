package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

@Service
public class HybridExperienceRequirementService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal ONE =
            BigDecimal.ONE;

    private static final BigDecimal CONFLICT_DIFFERENCE =
            new BigDecimal("0.10");

    private final ExperienceRequirementExtractorService
            ruleExtractorService;

    private final AiExperienceRequirementService
            aiExtractorService;

    private final BigDecimal minimumAiConfidence;

    private final BigDecimal trustedRuleConfidence;

    public HybridExperienceRequirementService(
            ExperienceRequirementExtractorService
                    ruleExtractorService,

            AiExperienceRequirementService
                    aiExtractorService,

            @Value("${jobfinder.experience.ai-min-confidence:0.78}")
            BigDecimal minimumAiConfidence,

            @Value("${jobfinder.experience.rule-trusted-confidence:0.85}")
            BigDecimal trustedRuleConfidence
    ) {
        this.ruleExtractorService =
                ruleExtractorService;

        this.aiExtractorService =
                aiExtractorService;

        this.minimumAiConfidence =
                normalizeThreshold(
                        minimumAiConfidence,
                        new BigDecimal("0.78")
                );

        this.trustedRuleConfidence =
                normalizeThreshold(
                        trustedRuleConfidence,
                        new BigDecimal("0.85")
                );
    }

    public ExperienceRequirementExtractorService
            .ExperienceRequirement extract(
            String jobTitle,
            String jobDescription
    ) {
        ExperienceRequirementExtractorService
                .ExperienceRequirement ruleResult =
                safeRuleResult(
                        ruleExtractorService.extract(
                                jobTitle,
                                jobDescription
                        )
                );

        if (!shouldRequestAiReview(
                ruleResult,
                jobDescription
        )) {
            return ruleResult;
        }

        Optional<AiExperienceRequirementService
                .AiExperienceRequirement> aiOptional =
                aiExtractorService.extract(
                        jobTitle,
                        jobDescription
                );

        if (aiOptional.isEmpty()) {
            return ruleResult;
        }

        ExperienceRequirementExtractorService
                .ExperienceRequirement aiResult =
                safeAiResult(
                        aiOptional.get()
                                .toExperienceRequirement()
                );

        if (confidenceOf(aiResult)
                .compareTo(
                        minimumAiConfidence
                ) < 0) {
            return ruleResult;
        }

        return selectFinalResult(
                ruleResult,
                aiResult
        );
    }

    private boolean shouldRequestAiReview(
            ExperienceRequirementExtractorService
                    .ExperienceRequirement ruleResult,
            String jobDescription
    ) {
        if (!aiExtractorService.isEnabled()) {
            return false;
        }

        ExperienceRequirementType type =
                safeType(
                        ruleResult.requirementType()
                );

        if (type
                == ExperienceRequirementType.AMBIGUOUS) {
            return true;
        }

        if (type
                == ExperienceRequirementType.NOT_SPECIFIED) {

            /*
             * Avoid unnecessary AI calls when the description
             * contains no experience-related language.
             */
            return containsPossibleExperienceLanguage(
                    jobDescription
            );
        }

        return confidenceOf(ruleResult)
                .compareTo(
                        trustedRuleConfidence
                ) < 0;
    }

    private ExperienceRequirementExtractorService
            .ExperienceRequirement selectFinalResult(
            ExperienceRequirementExtractorService
                    .ExperienceRequirement ruleResult,

            ExperienceRequirementExtractorService
                    .ExperienceRequirement aiResult
    ) {
        ExperienceRequirementType ruleType =
                safeType(
                        ruleResult.requirementType()
                );

        ExperienceRequirementType aiType =
                safeType(
                        aiResult.requirementType()
                );

        if (resultsAgree(
                ruleResult,
                aiResult
        )) {
            return mergeAgreedResults(
                    ruleResult,
                    aiResult
            );
        }

        /*
         * The rule engine found nothing, but AI found a
         * well-supported experience requirement.
         */
        if (ruleType
                == ExperienceRequirementType.NOT_SPECIFIED
                && aiType
                != ExperienceRequirementType.NOT_SPECIFIED) {

            return withMethod(
                    aiResult,
                    "AI_FALLBACK"
            );
        }

        /*
         * AI confirms that no experience was specified.
         */
        if (ruleType
                == ExperienceRequirementType.NOT_SPECIFIED
                && aiType
                == ExperienceRequirementType.NOT_SPECIFIED) {

            return new ExperienceRequirementExtractorService
                    .ExperienceRequirement(
                    null,
                    null,
                    ExperienceRequirementType
                            .NOT_SPECIFIED,
                    null,
                    confidenceOf(aiResult),
                    "RULE_AI"
            );
        }

        /*
         * The rule result was ambiguous and AI produced a
         * clear, validated result.
         */
        if (ruleType
                == ExperienceRequirementType.AMBIGUOUS
                && aiType
                != ExperienceRequirementType.AMBIGUOUS) {

            return withMethod(
                    aiResult,
                    "AI_FALLBACK"
            );
        }

        /*
         * Keep the rule result when AI itself remains
         * ambiguous.
         */
        if (aiType
                == ExperienceRequirementType.AMBIGUOUS
                && ruleType
                != ExperienceRequirementType.AMBIGUOUS) {

            return ruleResult;
        }

        BigDecimal ruleConfidence =
                confidenceOf(ruleResult);

        BigDecimal aiConfidence =
                confidenceOf(aiResult);

        BigDecimal confidenceDifference =
                aiConfidence.subtract(
                        ruleConfidence
                );

        if (confidenceDifference.compareTo(
                CONFLICT_DIFFERENCE
        ) >= 0) {

            return withMethod(
                    aiResult,
                    "AI_FALLBACK"
            );
        }

        if (confidenceDifference.compareTo(
                CONFLICT_DIFFERENCE.negate()
        ) <= 0) {

            return ruleResult;
        }

        /*
         * Both extractors produced different answers with
         * similar confidence. Do not make a strict eligibility
         * decision from this result.
         */
        return createConflictResult(
                ruleResult,
                aiResult
        );
    }

    private boolean resultsAgree(
            ExperienceRequirementExtractorService
                    .ExperienceRequirement first,

            ExperienceRequirementExtractorService
                    .ExperienceRequirement second
    ) {
        return safeType(
                first.requirementType()
        ) == safeType(
                second.requirementType()
        )
                && decimalEquals(
                first.minimumYears(),
                second.minimumYears()
        )
                && decimalEquals(
                first.maximumYears(),
                second.maximumYears()
        );
    }

    private ExperienceRequirementExtractorService
            .ExperienceRequirement mergeAgreedResults(
            ExperienceRequirementExtractorService
                    .ExperienceRequirement ruleResult,

            ExperienceRequirementExtractorService
                    .ExperienceRequirement aiResult
    ) {
        return new ExperienceRequirementExtractorService
                .ExperienceRequirement(
                firstNonNull(
                        ruleResult.minimumYears(),
                        aiResult.minimumYears()
                ),
                firstNonNull(
                        ruleResult.maximumYears(),
                        aiResult.maximumYears()
                ),
                safeType(
                        ruleResult.requirementType()
                ),
                preferredEvidence(
                        ruleResult.evidence(),
                        aiResult.evidence()
                ),
                confidenceOf(ruleResult)
                        .max(
                                confidenceOf(aiResult)
                        ),
                "RULE_AI"
        );
    }

    private ExperienceRequirementExtractorService
            .ExperienceRequirement createConflictResult(
            ExperienceRequirementExtractorService
                    .ExperienceRequirement ruleResult,

            ExperienceRequirementExtractorService
                    .ExperienceRequirement aiResult
    ) {
        BigDecimal minimum =
                matchingDecimalOrNull(
                        ruleResult.minimumYears(),
                        aiResult.minimumYears()
                );

        BigDecimal maximum =
                matchingDecimalOrNull(
                        ruleResult.maximumYears(),
                        aiResult.maximumYears()
                );

        BigDecimal confidence =
                confidenceOf(ruleResult)
                        .min(
                                confidenceOf(aiResult)
                        )
                        .min(
                                new BigDecimal("0.75")
                        );

        return new ExperienceRequirementExtractorService
                .ExperienceRequirement(
                minimum,
                maximum,
                ExperienceRequirementType.AMBIGUOUS,
                combineEvidence(
                        ruleResult.evidence(),
                        aiResult.evidence()
                ),
                confidence,
                "RULE_AI_CONFLICT"
        );
    }

    private ExperienceRequirementExtractorService
            .ExperienceRequirement withMethod(
            ExperienceRequirementExtractorService
                    .ExperienceRequirement result,
            String method
    ) {
        return new ExperienceRequirementExtractorService
                .ExperienceRequirement(
                result.minimumYears(),
                result.maximumYears(),
                safeType(
                        result.requirementType()
                ),
                cleanNullable(
                        result.evidence()
                ),
                confidenceOf(result),
                method
        );
    }

    private ExperienceRequirementExtractorService
            .ExperienceRequirement safeRuleResult(
            ExperienceRequirementExtractorService
                    .ExperienceRequirement result
    ) {
        if (result == null) {
            return notSpecified(
                    "RULE"
            );
        }

        return new ExperienceRequirementExtractorService
                .ExperienceRequirement(
                result.minimumYears(),
                result.maximumYears(),
                safeType(
                        result.requirementType()
                ),
                cleanNullable(
                        result.evidence()
                ),
                confidenceOf(result),
                StringUtils.hasText(
                        result.extractionMethod()
                )
                        ? result
                        .extractionMethod()
                        .trim()
                        : "RULE"
        );
    }

    private ExperienceRequirementExtractorService
            .ExperienceRequirement safeAiResult(
            ExperienceRequirementExtractorService
                    .ExperienceRequirement result
    ) {
        if (result == null) {
            return notSpecified(
                    "AI"
            );
        }

        return new ExperienceRequirementExtractorService
                .ExperienceRequirement(
                result.minimumYears(),
                result.maximumYears(),
                safeType(
                        result.requirementType()
                ),
                cleanNullable(
                        result.evidence()
                ),
                confidenceOf(result),
                "AI"
        );
    }

    private ExperienceRequirementExtractorService
            .ExperienceRequirement notSpecified(
            String method
    ) {
        return new ExperienceRequirementExtractorService
                .ExperienceRequirement(
                null,
                null,
                ExperienceRequirementType.NOT_SPECIFIED,
                null,
                ZERO,
                method
        );
    }

    private boolean containsPossibleExperienceLanguage(
            String jobDescription
    ) {
        if (!StringUtils.hasText(jobDescription)) {
            return false;
        }

        String normalized =
                jobDescription
                        .toLowerCase(
                                Locale.ROOT
                        );

        return normalized.contains("experience")
                || normalized.contains("years")
                || normalized.contains("year ")
                || normalized.contains("yrs")
                || normalized.contains("minimum")
                || normalized.contains("preferred")
                || normalized.contains("required")
                || normalized.contains("must have")
                || normalized.contains("qualification");
    }

    private ExperienceRequirementType safeType(
            ExperienceRequirementType type
    ) {
        return type == null
                ? ExperienceRequirementType.NOT_SPECIFIED
                : type;
    }

    private BigDecimal confidenceOf(
            ExperienceRequirementExtractorService
                    .ExperienceRequirement result
    ) {
        if (result == null
                || result.confidence() == null) {
            return ZERO;
        }

        if (result.confidence()
                .compareTo(ZERO) < 0) {
            return ZERO;
        }

        if (result.confidence()
                .compareTo(ONE) > 0) {
            return ONE;
        }

        return result.confidence();
    }

    private BigDecimal normalizeThreshold(
            BigDecimal value,
            BigDecimal defaultValue
    ) {
        if (value == null) {
            return defaultValue;
        }

        if (value.compareTo(ZERO) < 0
                || value.compareTo(ONE) > 0) {
            return defaultValue;
        }

        return value;
    }

    private boolean decimalEquals(
            BigDecimal first,
            BigDecimal second
    ) {
        if (first == null
                && second == null) {
            return true;
        }

        if (first == null
                || second == null) {
            return false;
        }

        return first.compareTo(second) == 0;
    }

    private BigDecimal matchingDecimalOrNull(
            BigDecimal first,
            BigDecimal second
    ) {
        return decimalEquals(
                first,
                second
        )
                ? first
                : null;
    }

    private BigDecimal firstNonNull(
            BigDecimal first,
            BigDecimal second
    ) {
        return first != null
                ? first
                : second;
    }

    private String preferredEvidence(
            String ruleEvidence,
            String aiEvidence
    ) {
        if (StringUtils.hasText(
                ruleEvidence
        )) {
            return ruleEvidence.trim();
        }

        return cleanNullable(
                aiEvidence
        );
    }

    private String combineEvidence(
            String ruleEvidence,
            String aiEvidence
    ) {
        String first =
                cleanNullable(
                        ruleEvidence
                );

        String second =
                cleanNullable(
                        aiEvidence
                );

        if (first == null) {
            return second;
        }

        if (second == null
                || first.equalsIgnoreCase(second)) {
            return first;
        }

        String combined =
                "Rule evidence: "
                        + first
                        + " | AI evidence: "
                        + second;

        return combined.length() <= 1000
                ? combined
                : combined.substring(
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