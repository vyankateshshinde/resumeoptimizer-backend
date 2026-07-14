package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceRequirementExtractorServiceTest {

    private ExperienceRequirementExtractorService extractorService;

    @BeforeEach
    void setUp() {
        extractorService =
                new ExperienceRequirementExtractorService();
    }

    @Test
    void shouldExtractRequiredExperienceRange() {
        String description =
                "Minimum 3 to 5 years of overall experience is required.";

        ExperienceRequirementExtractorService.ExperienceRequirement result =
                extractorService.extract(
                        "Java Developer",
                        description
                );

        assertAll(
                () -> assertEquals(
                        new BigDecimal("3"),
                        result.minimumYears()
                ),
                () -> assertEquals(
                        new BigDecimal("5"),
                        result.maximumYears()
                ),
                () -> assertEquals(
                        ExperienceRequirementType.REQUIRED,
                        result.requirementType()
                ),
                () -> assertEquals(
                        "RULE",
                        result.extractionMethod()
                ),
                () -> assertTrue(
                        result.isStrictRequirement()
                ),
                () -> assertTrue(
                        result.isSpecified()
                ),
                () -> assertTrue(
                        result.confidence()
                                .compareTo(
                                        new BigDecimal("0.90")
                                ) >= 0
                )
        );
    }

    @Test
    void shouldExtractRequiredPlusExperience() {
        String description =
                "The candidate must have 5+ years of Java development experience.";

        ExperienceRequirementExtractorService.ExperienceRequirement result =
                extractorService.extract(
                        "Senior Java Developer",
                        description
                );

        assertAll(
                () -> assertEquals(
                        new BigDecimal("5"),
                        result.minimumYears()
                ),
                () -> assertNull(
                        result.maximumYears()
                ),
                () -> assertEquals(
                        ExperienceRequirementType.REQUIRED,
                        result.requirementType()
                ),
                () -> assertTrue(
                        result.evidence()
                                .contains("5+ years")
                )
        );
    }

    @Test
    void shouldClassifyPreferredExperienceWithoutRejectingIt() {
        String description =
                "Five years is not mandatory. "
                        + "5 years of professional experience is preferred.";

        ExperienceRequirementExtractorService.ExperienceRequirement result =
                extractorService.extract(
                        "Backend Developer",
                        description
                );

        assertAll(
                () -> assertEquals(
                        new BigDecimal("5"),
                        result.minimumYears()
                ),
                () -> assertEquals(
                        ExperienceRequirementType.PREFERRED,
                        result.requirementType()
                ),
                () -> assertFalse(
                        result.isStrictRequirement()
                ),
                () -> assertTrue(
                        result.isSpecified()
                )
        );
    }

    @Test
    void shouldMarkConflictingRequirementLanguageAsAmbiguous() {
        String description =
                "Minimum 4 years of experience is required and preferred.";

        ExperienceRequirementExtractorService.ExperienceRequirement result =
                extractorService.extract(
                        "Software Engineer",
                        description
                );

        assertAll(
                () -> assertEquals(
                        new BigDecimal("4"),
                        result.minimumYears()
                ),
                () -> assertEquals(
                        ExperienceRequirementType.AMBIGUOUS,
                        result.requirementType()
                ),
                () -> assertTrue(
                        result.requiresAiReview()
                ),
                () -> assertFalse(
                        result.isStrictRequirement()
                )
        );
    }

    @Test
    void shouldRecognizeJobsWhereExperienceIsNotRequired() {
        String description =
                "Experience is not required. Freshers are welcome.";

        ExperienceRequirementExtractorService.ExperienceRequirement result =
                extractorService.extract(
                        "Junior Java Developer",
                        description
                );

        assertAll(
                () -> assertEquals(
                        BigDecimal.ZERO,
                        result.minimumYears()
                ),
                () -> assertNull(
                        result.maximumYears()
                ),
                () -> assertEquals(
                        ExperienceRequirementType.REQUIRED,
                        result.requirementType()
                ),
                () -> assertEquals(
                        new BigDecimal("0.99"),
                        result.confidence()
                ),
                () -> assertTrue(
                        result.evidence()
                                .toLowerCase()
                                .contains("experience")
                )
        );
    }

    @Test
    void shouldReturnNotSpecifiedWhenNoExperienceExists() {
        String description =
                "Design and develop REST APIs using Java, Spring Boot and MySQL.";

        ExperienceRequirementExtractorService.ExperienceRequirement result =
                extractorService.extract(
                        "Java Developer",
                        description
                );

        assertAll(
                () -> assertNull(
                        result.minimumYears()
                ),
                () -> assertNull(
                        result.maximumYears()
                ),
                () -> assertEquals(
                        ExperienceRequirementType.NOT_SPECIFIED,
                        result.requirementType()
                ),
                () -> assertEquals(
                        BigDecimal.ZERO,
                        result.confidence()
                ),
                () -> assertFalse(
                        result.isSpecified()
                ),
                () -> assertTrue(
                        result.requiresAiReview()
                )
        );
    }

    @Test
    void shouldIgnoreCompanyAgeAsCandidateExperience() {
        String description =
                "The company was founded in 2014 and has more than "
                        + "10 years of market presence.";

        ExperienceRequirementExtractorService.ExperienceRequirement result =
                extractorService.extract(
                        "Java Engineer",
                        description
                );

        assertAll(
                () -> assertNull(
                        result.minimumYears()
                ),
                () -> assertNull(
                        result.maximumYears()
                ),
                () -> assertEquals(
                        ExperienceRequirementType.NOT_SPECIFIED,
                        result.requirementType()
                )
        );
    }

    @Test
    void shouldHandleEmptyDescriptionSafely() {
        ExperienceRequirementExtractorService.ExperienceRequirement result =
                extractorService.extract(
                        null,
                        null
                );

        assertAll(
                () -> assertEquals(
                        ExperienceRequirementType.NOT_SPECIFIED,
                        result.requirementType()
                ),
                () -> assertNull(
                        result.minimumYears()
                ),
                () -> assertNull(
                        result.maximumYears()
                ),
                () -> assertEquals(
                        BigDecimal.ZERO,
                        result.confidence()
                )
        );
    }
}