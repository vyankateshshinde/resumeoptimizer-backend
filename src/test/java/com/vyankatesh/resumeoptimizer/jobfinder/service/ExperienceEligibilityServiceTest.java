package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceEligibilityServiceTest {

    private ExperienceEligibilityService
            experienceEligibilityService;

    @BeforeEach
    void setUp() {
        experienceEligibilityService =
                new ExperienceEligibilityService();
    }

    @Test
    void shouldRejectCandidateBelowRequiredMinimum() {
        JobListingEntity job =
                createJob(
                        ExperienceRequirementType.REQUIRED,
                        "4.0",
                        null
                );

        ExperienceEligibilityService.ExperienceEligibility result =
                experienceEligibilityService.evaluate(
                        new BigDecimal("3.7"),
                        job
                );

        assertAll(
                () -> assertFalse(
                        result.eligible()
                ),
                () -> assertEquals(
                        ExperienceEligibilityService
                                .ExperienceEligibilityStatus
                                .BELOW_REQUIRED_MINIMUM,
                        result.status()
                ),
                () -> assertTrue(
                        result.message().contains(
                                "requires at least 4"
                        )
                )
        );
    }

    @Test
    void shouldAcceptCandidateMeetingRequiredMinimumExactly() {
        JobListingEntity job =
                createJob(
                        ExperienceRequirementType.REQUIRED,
                        "3.7",
                        "5.0"
                );

        ExperienceEligibilityService.ExperienceEligibility result =
                experienceEligibilityService.evaluate(
                        new BigDecimal("3.7"),
                        job
                );

        assertAll(
                () -> assertTrue(
                        result.eligible()
                ),
                () -> assertEquals(
                        ExperienceEligibilityService
                                .ExperienceEligibilityStatus
                                .ELIGIBLE,
                        result.status()
                )
        );
    }

    @Test
    void shouldKeepPreferredJobWhenCandidateIsBelowPreference() {
        JobListingEntity job =
                createJob(
                        ExperienceRequirementType.PREFERRED,
                        "5.0",
                        null
                );

        ExperienceEligibilityService.ExperienceEligibility result =
                experienceEligibilityService.evaluate(
                        new BigDecimal("3.7"),
                        job
                );

        assertAll(
                () -> assertTrue(
                        result.eligible()
                ),
                () -> assertEquals(
                        ExperienceEligibilityService
                                .ExperienceEligibilityStatus
                                .BELOW_PREFERRED_MINIMUM,
                        result.status()
                ),
                () -> assertTrue(
                        result.message().contains(
                                "remains eligible"
                        )
                )
        );
    }

    @Test
    void shouldKeepAmbiguousJobAndRequestVerification() {
        JobListingEntity job =
                createJob(
                        ExperienceRequirementType.AMBIGUOUS,
                        "5.0",
                        null
                );

        ExperienceEligibilityService.ExperienceEligibility result =
                experienceEligibilityService.evaluate(
                        new BigDecimal("3.7"),
                        job
                );

        assertAll(
                () -> assertTrue(
                        result.eligible()
                ),
                () -> assertEquals(
                        ExperienceEligibilityService
                                .ExperienceEligibilityStatus
                                .VERIFICATION_REQUIRED,
                        result.status()
                ),
                () -> assertTrue(
                        result.message().contains(
                                "verification is required"
                        )
                )
        );
    }

    @Test
    void shouldKeepJobWhenExperienceIsNotSpecified() {
        JobListingEntity job =
                createJob(
                        ExperienceRequirementType.NOT_SPECIFIED,
                        null,
                        null
                );

        ExperienceEligibilityService.ExperienceEligibility result =
                experienceEligibilityService.evaluate(
                        new BigDecimal("3.7"),
                        job
                );

        assertAll(
                () -> assertTrue(
                        result.eligible()
                ),
                () -> assertEquals(
                        ExperienceEligibilityService
                                .ExperienceEligibilityStatus
                                .NOT_SPECIFIED,
                        result.status()
                )
        );
    }

    @Test
    void shouldNotRejectCandidateAboveListedMaximum() {
        JobListingEntity job =
                createJob(
                        ExperienceRequirementType.REQUIRED,
                        "2.0",
                        "4.0"
                );

        ExperienceEligibilityService.ExperienceEligibility result =
                experienceEligibilityService.evaluate(
                        new BigDecimal("5.0"),
                        job
                );

        assertAll(
                () -> assertTrue(
                        result.eligible()
                ),
                () -> assertEquals(
                        ExperienceEligibilityService
                                .ExperienceEligibilityStatus
                                .ABOVE_LISTED_MAXIMUM,
                        result.status()
                )
        );
    }

    private JobListingEntity createJob(
            ExperienceRequirementType requirementType,
            String minimumExperience,
            String maximumExperience
    ) {
        JobListingEntity job =
                new JobListingEntity();

        job.setExperienceRequirementType(
                requirementType
        );

        job.setMinimumExperience(
                toBigDecimal(
                        minimumExperience
                )
        );

        job.setMaximumExperience(
                toBigDecimal(
                        maximumExperience
                )
        );

        return job;
    }

    private BigDecimal toBigDecimal(
            String value
    ) {
        return value == null
                ? null
                : new BigDecimal(value);
    }
}