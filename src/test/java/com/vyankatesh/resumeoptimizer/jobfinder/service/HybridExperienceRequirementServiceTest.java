package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HybridExperienceRequirementServiceTest {

    @Mock
    private ExperienceRequirementExtractorService
            ruleExtractorService;

    @Mock
    private AiExperienceRequirementService
            aiExtractorService;

    private HybridExperienceRequirementService
            hybridService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        hybridService =
                new HybridExperienceRequirementService(
                        ruleExtractorService,
                        aiExtractorService,
                        new BigDecimal("0.78"),
                        new BigDecimal("0.85")
                );
    }

    @Test
    void shouldUseTrustedRuleResultWithoutCallingAi() {
        ExperienceRequirementExtractorService
                .ExperienceRequirement ruleResult =
                requirement(
                        "3",
                        "5",
                        ExperienceRequirementType.REQUIRED,
                        "Minimum 3 to 5 years required",
                        "0.95",
                        "RULE"
                );

        when(
                ruleExtractorService.extract(
                        anyString(),
                        anyString()
                )
        ).thenReturn(ruleResult);

        when(
                aiExtractorService.isEnabled()
        ).thenReturn(true);

        ExperienceRequirementExtractorService
                .ExperienceRequirement result =
                hybridService.extract(
                        "Java Developer",
                        "Minimum 3 to 5 years required"
                );

        assertAll(
                () -> assertEquals(
                        ExperienceRequirementType.REQUIRED,
                        result.requirementType()
                ),
                () -> assertEquals(
                        new BigDecimal("3"),
                        result.minimumYears()
                ),
                () -> assertEquals(
                        "RULE",
                        result.extractionMethod()
                )
        );

        verify(
                aiExtractorService,
                never()
        ).extract(
                anyString(),
                anyString()
        );
    }

    @Test
    void shouldUseAiFallbackForAmbiguousRuleResult() {
        ExperienceRequirementExtractorService
                .ExperienceRequirement ruleResult =
                requirement(
                        "3",
                        null,
                        ExperienceRequirementType.AMBIGUOUS,
                        "3 years preferred and required",
                        "0.60",
                        "RULE"
                );

        AiExperienceRequirementService
                .AiExperienceRequirement aiResult =
                aiRequirement(
                        "3",
                        null,
                        ExperienceRequirementType.REQUIRED,
                        "Minimum 3 years of experience is required",
                        "0.93"
                );

        when(
                ruleExtractorService.extract(
                        anyString(),
                        anyString()
                )
        ).thenReturn(ruleResult);

        when(
                aiExtractorService.isEnabled()
        ).thenReturn(true);

        when(
                aiExtractorService.extract(
                        anyString(),
                        anyString()
                )
        ).thenReturn(
                Optional.of(aiResult)
        );

        ExperienceRequirementExtractorService
                .ExperienceRequirement result =
                hybridService.extract(
                        "Java Developer",
                        "Minimum 3 years of experience is required"
                );

        assertAll(
                () -> assertEquals(
                        ExperienceRequirementType.REQUIRED,
                        result.requirementType()
                ),
                () -> assertEquals(
                        new BigDecimal("3"),
                        result.minimumYears()
                ),
                () -> assertEquals(
                        "AI_FALLBACK",
                        result.extractionMethod()
                )
        );
    }

    @Test
    void shouldNotCallAiWhenNoExperienceLanguageExists() {
        ExperienceRequirementExtractorService
                .ExperienceRequirement ruleResult =
                requirement(
                        null,
                        null,
                        ExperienceRequirementType.NOT_SPECIFIED,
                        null,
                        "0.00",
                        "RULE"
                );

        when(
                ruleExtractorService.extract(
                        anyString(),
                        anyString()
                )
        ).thenReturn(ruleResult);

        when(
                aiExtractorService.isEnabled()
        ).thenReturn(true);

        ExperienceRequirementExtractorService
                .ExperienceRequirement result =
                hybridService.extract(
                        "Java Developer",
                        "Build secure REST APIs using Spring Boot."
                );

        assertEquals(
                ExperienceRequirementType.NOT_SPECIFIED,
                result.requirementType()
        );

        verify(
                aiExtractorService,
                never()
        ).extract(
                anyString(),
                anyString()
        );
    }

    @Test
    void shouldMergeRuleAndAiWhenTheyAgree() {
        ExperienceRequirementExtractorService
                .ExperienceRequirement ruleResult =
                requirement(
                        "3",
                        null,
                        ExperienceRequirementType.REQUIRED,
                        "3 years of experience required",
                        "0.82",
                        "RULE"
                );

        AiExperienceRequirementService
                .AiExperienceRequirement aiResult =
                aiRequirement(
                        "3",
                        null,
                        ExperienceRequirementType.REQUIRED,
                        "3 years of experience required",
                        "0.92"
                );

        when(
                ruleExtractorService.extract(
                        anyString(),
                        anyString()
                )
        ).thenReturn(ruleResult);

        when(
                aiExtractorService.isEnabled()
        ).thenReturn(true);

        when(
                aiExtractorService.extract(
                        anyString(),
                        anyString()
                )
        ).thenReturn(
                Optional.of(aiResult)
        );

        ExperienceRequirementExtractorService
                .ExperienceRequirement result =
                hybridService.extract(
                        "Java Developer",
                        "3 years of experience required"
                );

        assertAll(
                () -> assertEquals(
                        ExperienceRequirementType.REQUIRED,
                        result.requirementType()
                ),
                () -> assertEquals(
                        new BigDecimal("3"),
                        result.minimumYears()
                ),
                () -> assertEquals(
                        new BigDecimal("0.92"),
                        result.confidence()
                ),
                () -> assertEquals(
                        "RULE_AI",
                        result.extractionMethod()
                )
        );
    }

    @Test
    void shouldMarkSimilarConfidenceConflictAsAmbiguous() {
        ExperienceRequirementExtractorService
                .ExperienceRequirement ruleResult =
                requirement(
                        "3",
                        null,
                        ExperienceRequirementType.REQUIRED,
                        "3 years required",
                        "0.82",
                        "RULE"
                );

        AiExperienceRequirementService
                .AiExperienceRequirement aiResult =
                aiRequirement(
                        "5",
                        null,
                        ExperienceRequirementType.PREFERRED,
                        "5 years preferred",
                        "0.85"
                );

        when(
                ruleExtractorService.extract(
                        anyString(),
                        anyString()
                )
        ).thenReturn(ruleResult);

        when(
                aiExtractorService.isEnabled()
        ).thenReturn(true);

        when(
                aiExtractorService.extract(
                        anyString(),
                        anyString()
                )
        ).thenReturn(
                Optional.of(aiResult)
        );

        ExperienceRequirementExtractorService
                .ExperienceRequirement result =
                hybridService.extract(
                        "Java Developer",
                        "3 years required. 5 years preferred."
                );

        assertAll(
                () -> assertEquals(
                        ExperienceRequirementType.AMBIGUOUS,
                        result.requirementType()
                ),
                () -> assertNull(
                        result.minimumYears()
                ),
                () -> assertEquals(
                        "RULE_AI_CONFLICT",
                        result.extractionMethod()
                )
        );
    }

    private ExperienceRequirementExtractorService
            .ExperienceRequirement requirement(
            String minimum,
            String maximum,
            ExperienceRequirementType type,
            String evidence,
            String confidence,
            String method
    ) {
        return new ExperienceRequirementExtractorService
                .ExperienceRequirement(
                decimal(minimum),
                decimal(maximum),
                type,
                evidence,
                new BigDecimal(confidence),
                method
        );
    }

    private AiExperienceRequirementService
            .AiExperienceRequirement aiRequirement(
            String minimum,
            String maximum,
            ExperienceRequirementType type,
            String evidence,
            String confidence
    ) {
        return new AiExperienceRequirementService
                .AiExperienceRequirement(
                decimal(minimum),
                decimal(maximum),
                type,
                evidence,
                new BigDecimal(confidence),
                "AI"
        );
    }

    private BigDecimal decimal(
            String value
    ) {
        return value == null
                ? null
                : new BigDecimal(value);
    }
}