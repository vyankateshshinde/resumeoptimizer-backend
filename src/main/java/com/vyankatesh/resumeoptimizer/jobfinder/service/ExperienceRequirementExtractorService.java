package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExperienceRequirementExtractorService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal MAXIMUM_REASONABLE_EXPERIENCE =
            new BigDecimal("40");

    /*
     * Matches:
     * 3-5 years
     * 3 – 5 years
     * 3 to 5 years
     * 3 through 5 yrs
     */
    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "(?<!\\d)"
                    + "(\\d{1,2}(?:\\.\\d+)?)"
                    + "\\s*(?:-|–|—|to|through)\\s*"
                    + "(\\d{1,2}(?:\\.\\d+)?)"
                    + "\\s*(?:years?|yrs?)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /*
     * Matches:
     * 5+ years
     * 3.5+ yrs
     */
    private static final Pattern PLUS_PATTERN = Pattern.compile(
            "(?<!\\d)"
                    + "(\\d{1,2}(?:\\.\\d+)?)"
                    + "\\s*\\+\\s*"
                    + "(?:years?|yrs?)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /*
     * Matches:
     * minimum 5 years
     * minimum of 5 years
     * at least 5 years
     * not less than 5 years
     * more than 5 years
     * over 5 years
     */
    private static final Pattern MINIMUM_PATTERN = Pattern.compile(
            "\\b(?:minimum(?:\\s+of)?|"
                    + "at\\s+least|"
                    + "not\\s+less\\s+than|"
                    + "more\\s+than|"
                    + "over)"
                    + "\\s*"
                    + "(\\d{1,2}(?:\\.\\d+)?)"
                    + "\\s*(?:years?|yrs?)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /*
     * Matches:
     * 5 years of experience
     * 5 years relevant experience
     * 5 years professional experience
     * 5 years overall experience
     */
    private static final Pattern YEARS_OF_EXPERIENCE_PATTERN =
            Pattern.compile(
                    "(?<!\\d)"
                            + "(\\d{1,2}(?:\\.\\d+)?)"
                            + "\\s*(?:\\+\\s*)?"
                            + "(?:years?|yrs?)"
                            + "\\s+(?:of\\s+)?"
                            + "(?:(?:relevant|overall|total|professional|"
                            + "industry|software|development|engineering)"
                            + "\\s+){0,3}"
                            + "experience\\b",
                    Pattern.CASE_INSENSITIVE
            );

    /*
     * Matches:
     * experience: 5 years
     * experience required: 5 years
     * overall experience of 5 years
     * minimum experience 5 years
     */
    private static final Pattern EXPERIENCE_FIRST_PATTERN =
            Pattern.compile(
                    "\\b"
                            + "(?:(?:minimum|overall|total|relevant|professional)"
                            + "\\s+)?"
                            + "experience"
                            + "(?:\\s+(?:required|needed|expected))?"
                            + "\\s*(?:of|:|-)?\\s*"
                            + "(\\d{1,2}(?:\\.\\d+)?)"
                            + "\\s*(?:\\+\\s*)?"
                            + "(?:years?|yrs?)\\b",
                    Pattern.CASE_INSENSITIVE
            );

    /*
     * Matches:
     * 3 years in Java
     * 4 years with Spring Boot
     * 5 years as a software engineer
     * 3 years working with microservices
     */
    private static final Pattern YEARS_IN_ROLE_PATTERN =
            Pattern.compile(
                    "(?<!\\d)"
                            + "(\\d{1,2}(?:\\.\\d+)?)"
                            + "\\s*(?:\\+\\s*)?"
                            + "(?:years?|yrs?)"
                            + "\\s+(?:in|with|using|as|working\\s+with|"
                            + "working\\s+on|developing|building)"
                            + "\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern NO_EXPERIENCE_PATTERN =
            Pattern.compile(
                    "\\b(?:"
                            + "no\\s+(?:prior\\s+|previous\\s+)?"
                            + "experience\\s+(?:is\\s+)?required"
                            + "|experience\\s+(?:is\\s+)?not\\s+required"
                            + "|freshers?\\s+(?:are\\s+)?"
                            + "(?:welcome|eligible|encouraged)"
                            + "|entry[- ]level\\s+(?:role|position|opportunity)"
                            + ")\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final List<String> REQUIRED_TERMS = List.of(
            "required",
            "requirement",
            "must have",
            "must possess",
            "mandatory",
            "minimum",
            "at least",
            "essential",
            "should have",
            "need to have",
            "expected to have",
            "basic qualification",
            "minimum qualification"
    );

    private static final List<String> PREFERRED_TERMS = List.of(
            "preferred",
            "preferably",
            "desirable",
            "desired",
            "nice to have",
            "good to have",
            "added advantage",
            "would be a plus",
            "is a plus",
            "bonus",
            "advantageous",
            "optional"
    );

    private static final List<String> OVERALL_EXPERIENCE_TERMS = List.of(
            "overall experience",
            "total experience",
            "professional experience",
            "industry experience",
            "software development experience",
            "software engineering experience"
    );

    private static final List<String> EXPERIENCE_CONTEXT_TERMS = List.of(
            "experience",
            "experienced",
            "years in",
            "years with",
            "years using",
            "years as",
            "years working",
            "years developing",
            "years building"
    );

    /*
     * These contexts frequently contain year values that are not
     * candidate-experience requirements.
     */
    private static final List<String> EXCLUDED_CONTEXT_TERMS = List.of(
            "company founded",
            "founded in",
            "established in",
            "years old",
            "age requirement",
            "contract duration",
            "contract period",
            "project duration",
            "program duration",
            "course duration",
            "warranty period",
            "bond period",
            "graduated within",
            "graduation within",
            "completed degree within",
            "degree duration",
            "bachelor's degree of",
            "four year degree",
            "4 year degree",
            "three year degree",
            "3 year degree",
            "within the last",
            "in the past"
    );

    public ExperienceRequirement extract(
            String jobTitle,
            String jobDescription
    ) {
        String completeText =
                safe(jobTitle)
                        + "\n"
                        + safe(jobDescription);

        return extract(completeText);
    }

    public ExperienceRequirement extract(String text) {
        if (!StringUtils.hasText(text)) {
            return ExperienceRequirement.notSpecified();
        }

        String cleanedText = cleanHtml(text);

        List<TextSegment> segments =
                createSegments(cleanedText);

        ExperienceRequirement noExperienceResult =
                findNoExperienceRequirement(segments);

        if (noExperienceResult != null) {
            return noExperienceResult;
        }

        List<Candidate> candidates =
                new ArrayList<>();

        for (TextSegment segment : segments) {
            String normalized =
                    segment.normalizedText();

            if (!containsExperienceContext(normalized)) {
                continue;
            }

            if (isExcludedContext(normalized)
                    && !containsAny(
                    normalized,
                    OVERALL_EXPERIENCE_TERMS
            )) {
                continue;
            }

            collectRangeCandidates(
                    segment,
                    candidates
            );

            collectMinimumCandidates(
                    segment,
                    candidates
            );

            collectPlusCandidates(
                    segment,
                    candidates
            );

            collectYearsOfExperienceCandidates(
                    segment,
                    candidates
            );

            collectExperienceFirstCandidates(
                    segment,
                    candidates
            );

            collectYearsInRoleCandidates(
                    segment,
                    candidates
            );
        }

        List<Candidate> uniqueCandidates =
                removeDuplicates(candidates);

        if (uniqueCandidates.isEmpty()) {
            return ExperienceRequirement.notSpecified();
        }

        return selectFinalRequirement(
                uniqueCandidates
        );
    }

    private void collectRangeCandidates(
            TextSegment segment,
            List<Candidate> candidates
    ) {
        Matcher matcher =
                RANGE_PATTERN.matcher(
                        segment.normalizedText()
                );

        while (matcher.find()) {
            BigDecimal first =
                    parseYears(matcher.group(1));

            BigDecimal second =
                    parseYears(matcher.group(2));

            if (!isValidExperience(first)
                    || !isValidExperience(second)) {
                continue;
            }

            BigDecimal minimum =
                    first.min(second);

            BigDecimal maximum =
                    first.max(second);

            candidates.add(
                    createCandidate(
                            minimum,
                            maximum,
                            segment,
                            95
                    )
            );
        }
    }

    private void collectMinimumCandidates(
            TextSegment segment,
            List<Candidate> candidates
    ) {
        Matcher matcher =
                MINIMUM_PATTERN.matcher(
                        segment.normalizedText()
                );

        while (matcher.find()) {
            BigDecimal minimum =
                    parseYears(matcher.group(1));

            if (!isValidExperience(minimum)) {
                continue;
            }

            candidates.add(
                    createCandidate(
                            minimum,
                            null,
                            segment,
                            98
                    )
            );
        }
    }

    private void collectPlusCandidates(
            TextSegment segment,
            List<Candidate> candidates
    ) {
        Matcher matcher =
                PLUS_PATTERN.matcher(
                        segment.normalizedText()
                );

        while (matcher.find()) {
            BigDecimal minimum =
                    parseYears(matcher.group(1));

            if (!isValidExperience(minimum)) {
                continue;
            }

            candidates.add(
                    createCandidate(
                            minimum,
                            null,
                            segment,
                            94
                    )
            );
        }
    }

    private void collectYearsOfExperienceCandidates(
            TextSegment segment,
            List<Candidate> candidates
    ) {
        Matcher matcher =
                YEARS_OF_EXPERIENCE_PATTERN.matcher(
                        segment.normalizedText()
                );

        while (matcher.find()) {
            BigDecimal minimum =
                    parseYears(matcher.group(1));

            if (!isValidExperience(minimum)) {
                continue;
            }

            candidates.add(
                    createCandidate(
                            minimum,
                            null,
                            segment,
                            90
                    )
            );
        }
    }

    private void collectExperienceFirstCandidates(
            TextSegment segment,
            List<Candidate> candidates
    ) {
        Matcher matcher =
                EXPERIENCE_FIRST_PATTERN.matcher(
                        segment.normalizedText()
                );

        while (matcher.find()) {
            BigDecimal minimum =
                    parseYears(matcher.group(1));

            if (!isValidExperience(minimum)) {
                continue;
            }

            candidates.add(
                    createCandidate(
                            minimum,
                            null,
                            segment,
                            90
                    )
            );
        }
    }

    private void collectYearsInRoleCandidates(
            TextSegment segment,
            List<Candidate> candidates
    ) {
        Matcher matcher =
                YEARS_IN_ROLE_PATTERN.matcher(
                        segment.normalizedText()
                );

        while (matcher.find()) {
            BigDecimal minimum =
                    parseYears(matcher.group(1));

            if (!isValidExperience(minimum)) {
                continue;
            }

            candidates.add(
                    createCandidate(
                            minimum,
                            null,
                            segment,
                            76
                    )
            );
        }
    }

    private Candidate createCandidate(
            BigDecimal minimum,
            BigDecimal maximum,
            TextSegment segment,
            int baseScore
    ) {
        String normalized =
                segment.normalizedText();

        boolean containsRequiredTerm =
                containsAny(
                        normalized,
                        REQUIRED_TERMS
                );

        boolean containsPreferredTerm =
                containsAny(
                        normalized,
                        PREFERRED_TERMS
                );

        boolean overallExperience =
                containsAny(
                        normalized,
                        OVERALL_EXPERIENCE_TERMS
                );

        ExperienceRequirementType type;

        if (containsRequiredTerm
                && containsPreferredTerm) {
            type =
                    ExperienceRequirementType.AMBIGUOUS;

        } else if (containsPreferredTerm) {
            type =
                    ExperienceRequirementType.PREFERRED;

        } else {
            /*
             * A qualification sentence such as
             * "5 years of Java experience" is treated as required
             * unless it explicitly says preferred or optional.
             */
            type =
                    ExperienceRequirementType.REQUIRED;
        }

        int score = baseScore;

        if (containsRequiredTerm) {
            score += 20;
        }

        if (overallExperience) {
            score += 30;
        }

        if (normalized.contains("minimum")
                || normalized.contains("at least")) {
            score += 15;
        }

        if (maximum != null) {
            score += 8;
        }

        if (type
                == ExperienceRequirementType.PREFERRED) {
            score -= 15;
        }

        if (type
                == ExperienceRequirementType.AMBIGUOUS) {
            score -= 25;
        }

        BigDecimal confidence =
                confidenceFor(
                        score,
                        type
                );

        return new Candidate(
                minimum,
                maximum,
                type,
                limitEvidence(
                        segment.originalText()
                ),
                confidence,
                score,
                overallExperience
        );
    }

    private ExperienceRequirement selectFinalRequirement(
            List<Candidate> candidates
    ) {
        List<Candidate> requiredCandidates =
                candidates.stream()
                        .filter(candidate ->
                                candidate.requirementType()
                                        == ExperienceRequirementType.REQUIRED
                        )
                        .toList();

        if (!requiredCandidates.isEmpty()) {
            return selectFromCandidates(
                    requiredCandidates,
                    true
            );
        }

        List<Candidate> ambiguousCandidates =
                candidates.stream()
                        .filter(candidate ->
                                candidate.requirementType()
                                        == ExperienceRequirementType.AMBIGUOUS
                        )
                        .toList();

        if (!ambiguousCandidates.isEmpty()) {
            Candidate selected =
                    bestCandidate(
                            ambiguousCandidates
                    );

            return toRequirement(
                    selected,
                    ExperienceRequirementType.AMBIGUOUS
            );
        }

        List<Candidate> preferredCandidates =
                candidates.stream()
                        .filter(candidate ->
                                candidate.requirementType()
                                        == ExperienceRequirementType.PREFERRED
                        )
                        .toList();

        if (!preferredCandidates.isEmpty()) {
            Candidate selected =
                    bestCandidate(
                            preferredCandidates
                    );

            return toRequirement(
                    selected,
                    ExperienceRequirementType.PREFERRED
            );
        }

        return ExperienceRequirement.notSpecified();
    }

    private ExperienceRequirement selectFromCandidates(
            List<Candidate> requiredCandidates,
            boolean detectConflicts
    ) {
        Candidate selected =
                bestCandidate(
                        requiredCandidates
                );

        if (selected == null) {
            return ExperienceRequirement.notSpecified();
        }

        if (detectConflicts
                && hasConflictingTopRequirements(
                selected,
                requiredCandidates
        )) {
            return new ExperienceRequirement(
                    selected.minimumYears(),
                    selected.maximumYears(),
                    ExperienceRequirementType.AMBIGUOUS,
                    selected.evidence(),
                    reduceConfidence(
                            selected.confidence()
                    ),
                    "RULE"
            );
        }

        return toRequirement(
                selected,
                ExperienceRequirementType.REQUIRED
        );
    }

    private Candidate bestCandidate(
            List<Candidate> candidates
    ) {
        return candidates.stream()
                .max(
                        Comparator
                                .comparingInt(
                                        Candidate::score
                                )
                                .thenComparing(
                                        Candidate::overallExperience
                                )
                                .thenComparing(
                                        Candidate::minimumYears
                                )
                )
                .orElse(null);
    }

    private boolean hasConflictingTopRequirements(
            Candidate selected,
            List<Candidate> candidates
    ) {
        for (Candidate candidate : candidates) {
            if (candidate == selected) {
                continue;
            }

            if (candidate.overallExperience()
                    || selected.overallExperience()) {
                continue;
            }

            int scoreDifference =
                    Math.abs(
                            selected.score()
                                    - candidate.score()
                    );

            boolean differentMinimum =
                    selected.minimumYears()
                            .compareTo(
                                    candidate.minimumYears()
                            ) != 0;

            if (scoreDifference <= 3
                    && differentMinimum) {
                return true;
            }
        }

        return false;
    }

    private ExperienceRequirement toRequirement(
            Candidate candidate,
            ExperienceRequirementType finalType
    ) {
        return new ExperienceRequirement(
                candidate.minimumYears(),
                candidate.maximumYears(),
                finalType,
                candidate.evidence(),
                candidate.confidence(),
                "RULE"
        );
    }

    private ExperienceRequirement findNoExperienceRequirement(
            List<TextSegment> segments
    ) {
        for (TextSegment segment : segments) {
            Matcher matcher =
                    NO_EXPERIENCE_PATTERN.matcher(
                            segment.normalizedText()
                    );

            if (matcher.find()) {
                return new ExperienceRequirement(
                        ZERO,
                        null,
                        ExperienceRequirementType.REQUIRED,
                        limitEvidence(
                                segment.originalText()
                        ),
                        new BigDecimal("0.99"),
                        "RULE"
                );
            }
        }

        return null;
    }

    private List<Candidate> removeDuplicates(
            List<Candidate> candidates
    ) {
        Map<String, Candidate> unique =
                new LinkedHashMap<>();

        for (Candidate candidate : candidates) {
            String key =
                    candidate.minimumYears()
                            + "|"
                            + candidate.maximumYears()
                            + "|"
                            + candidate.requirementType()
                            + "|"
                            + candidate.evidence();

            Candidate existing =
                    unique.get(key);

            if (existing == null
                    || candidate.score()
                    > existing.score()) {
                unique.put(
                        key,
                        candidate
                );
            }
        }

        return List.copyOf(
                unique.values()
        );
    }

    private BigDecimal confidenceFor(
            int score,
            ExperienceRequirementType type
    ) {
        BigDecimal confidence;

        if (score >= 135) {
            confidence =
                    new BigDecimal("0.98");

        } else if (score >= 115) {
            confidence =
                    new BigDecimal("0.94");

        } else if (score >= 95) {
            confidence =
                    new BigDecimal("0.90");

        } else if (score >= 80) {
            confidence =
                    new BigDecimal("0.82");

        } else {
            confidence =
                    new BigDecimal("0.72");
        }

        if (type
                == ExperienceRequirementType.PREFERRED) {
            confidence =
                    confidence.subtract(
                            new BigDecimal("0.03")
                    );
        }

        if (type
                == ExperienceRequirementType.AMBIGUOUS) {
            confidence =
                    confidence.subtract(
                            new BigDecimal("0.15")
                    );
        }

        if (confidence.compareTo(ZERO) < 0) {
            return ZERO;
        }

        if (confidence.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }

        return confidence;
    }

    private BigDecimal reduceConfidence(
            BigDecimal confidence
    ) {
        if (confidence == null) {
            return new BigDecimal("0.50");
        }

        BigDecimal reduced =
                confidence.subtract(
                        new BigDecimal("0.20")
                );

        return reduced.max(
                new BigDecimal("0.40")
        );
    }

    private List<TextSegment> createSegments(
            String text
    ) {
        String preparedText =
                text.replace("•", "\n")
                        .replace("●", "\n")
                        .replace("▪", "\n")
                        .replace("◦", "\n")
                        .replace("|", "\n");

        String[] parts =
                preparedText.split(
                        "(?<=[.!?;])\\s+|\\n+|\\r+"
                );

        List<TextSegment> segments =
                new ArrayList<>();

        for (String part : parts) {
            String original =
                    part.replaceAll("\\s+", " ")
                            .trim();

            if (original.isBlank()) {
                continue;
            }

            segments.add(
                    new TextSegment(
                            original,
                            normalize(original)
                    )
            );
        }

        return segments;
    }

    private boolean containsExperienceContext(
            String text
    ) {
        if (containsAny(
                text,
                EXPERIENCE_CONTEXT_TERMS
        )) {
            return true;
        }

        return RANGE_PATTERN.matcher(text).find()
                && text.contains("year");
    }

    private boolean isExcludedContext(
            String text
    ) {
        return containsAny(
                text,
                EXCLUDED_CONTEXT_TERMS
        );
    }

    private boolean containsAny(
            String text,
            List<String> values
    ) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }

        return false;
    }

    private BigDecimal parseYears(
            String value
    ) {
        try {
            return new BigDecimal(
                    value.trim()
            );
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean isValidExperience(
            BigDecimal value
    ) {
        return value != null
                && value.compareTo(ZERO) >= 0
                && value.compareTo(
                MAXIMUM_REASONABLE_EXPERIENCE
        ) <= 0;
    }

    private String cleanHtml(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return value
                .replaceAll(
                        "(?i)<br\\s*/?>",
                        "\n"
                )
                .replaceAll(
                        "(?i)</p>",
                        "\n"
                )
                .replaceAll(
                        "(?i)</li>",
                        "\n"
                )
                .replaceAll(
                        "(?i)</div>",
                        "\n"
                )
                .replaceAll(
                        "<[^>]+>",
                        " "
                )
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll(
                        "[\\t\\x0B\\f\\r ]+",
                        " "
                )
                .replaceAll(
                        "\\n\\s+",
                        "\n"
                )
                .replaceAll(
                        "\\n{3,}",
                        "\n\n"
                )
                .trim();
    }

    private String normalize(String value) {
        return safe(value)
                .toLowerCase(Locale.ROOT)
                .replace("yrs.", "years")
                .replace("yr.", "year")
                .replace("yrs", "years")
                .replace("yr", "year")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limitEvidence(
            String evidence
    ) {
        if (!StringUtils.hasText(evidence)) {
            return null;
        }

        String cleaned =
                evidence.trim();

        if (cleaned.length() <= 1000) {
            return cleaned;
        }

        return cleaned.substring(
                0,
                1000
        );
    }

    private String safe(String value) {
        return value == null
                ? ""
                : value;
    }

    private record TextSegment(
            String originalText,
            String normalizedText
    ) {
    }

    private record Candidate(
            BigDecimal minimumYears,
            BigDecimal maximumYears,
            ExperienceRequirementType requirementType,
            String evidence,
            BigDecimal confidence,
            int score,
            boolean overallExperience
    ) {
    }

    public record ExperienceRequirement(
            BigDecimal minimumYears,
            BigDecimal maximumYears,
            ExperienceRequirementType requirementType,
            String evidence,
            BigDecimal confidence,
            String extractionMethod
    ) {

        public static ExperienceRequirement notSpecified() {
            return new ExperienceRequirement(
                    null,
                    null,
                    ExperienceRequirementType.NOT_SPECIFIED,
                    null,
                    BigDecimal.ZERO,
                    "RULE"
            );
        }

        public boolean isSpecified() {
            return minimumYears != null
                    || maximumYears != null;
        }

        public boolean isStrictRequirement() {
            return requirementType
                    == ExperienceRequirementType.REQUIRED;
        }

        public boolean requiresAiReview() {
            return requirementType
                    == ExperienceRequirementType.AMBIGUOUS
                    || (
                    requirementType
                            == ExperienceRequirementType.NOT_SPECIFIED
                            && confidence.compareTo(
                            new BigDecimal("0.70")
                    ) < 0
            );
        }
    }
}