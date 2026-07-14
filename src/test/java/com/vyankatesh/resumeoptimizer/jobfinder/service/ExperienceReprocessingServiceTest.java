package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSource;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExperienceReprocessingServiceTest {

    @Mock
    private JobListingRepository repository;

    @Mock
    private HybridExperienceRequirementService hybridService;

    private ExperienceReprocessingService
            reprocessingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        reprocessingService =
                new ExperienceReprocessingService(
                        repository,
                        hybridService
                );
    }

    @Test
    void shouldPreserveProviderValuesAndReprocessOtherJobs() {
        JobListingEntity providerJob =
                createJob(
                        "Provider Java Job",
                        "Minimum 3 years required",
                        JobSource.HIMALAYAS
                );

        providerJob.setMinimumExperience(
                new BigDecimal("3")
        );

        providerJob.setExperienceRequirementType(
                ExperienceRequirementType.REQUIRED
        );

        providerJob.setExperienceExtractionMethod(
                "PROVIDER"
        );

        JobListingEntity extractedJob =
                createJob(
                        "Backend Developer",
                        "Five years of experience preferred",
                        JobSource.HIMALAYAS
                );

        when(
                repository.findAllByActiveTrueAndSource(
                        JobSource.HIMALAYAS
                )
        ).thenReturn(
                List.of(
                        providerJob,
                        extractedJob
                )
        );

        when(
                hybridService.extract(
                        extractedJob.getTitle(),
                        extractedJob.getDescription()
                )
        ).thenReturn(
                new ExperienceRequirementExtractorService
                        .ExperienceRequirement(
                        new BigDecimal("5"),
                        null,
                        ExperienceRequirementType.PREFERRED,
                        "Five years of experience preferred",
                        new BigDecimal("0.92"),
                        "RULE_AI"
                )
        );

        ExperienceReprocessingService.ReprocessingResult result =
                reprocessingService.reprocessJobsBySource(
                        JobSource.HIMALAYAS
                );

        assertAll(
                () -> assertEquals(
                        2,
                        result.scanned()
                ),
                () -> assertEquals(
                        1,
                        result.updated()
                ),
                () -> assertEquals(
                        1,
                        result.providerPreserved()
                ),
                () -> assertEquals(
                        1,
                        result.preferred()
                ),
                () -> assertEquals(
                        0,
                        result.failed()
                ),
                () -> assertEquals(
                        new BigDecimal("5"),
                        extractedJob.getMinimumExperience()
                ),
                () -> assertEquals(
                        ExperienceRequirementType.PREFERRED,
                        extractedJob.getExperienceRequirementType()
                ),
                () -> assertEquals(
                        "RULE_AI",
                        extractedJob.getExperienceExtractionMethod()
                )
        );

        verify(
                repository
        ).saveAll(
                anyList()
        );

        verify(
                repository
        ).flush();
    }

    @Test
    void shouldPreserveExistingValuesWhenExtractionFails() {
        JobListingEntity job =
                createJob(
                        "Java Developer",
                        "Minimum 2 years required",
                        JobSource.HIMALAYAS
                );

        job.setMinimumExperience(
                new BigDecimal("2")
        );

        job.setExperienceRequirementType(
                ExperienceRequirementType.REQUIRED
        );

        job.setExperienceExtractionMethod(
                "RULE"
        );

        when(
                repository.findAllByActiveTrueAndSource(
                        JobSource.HIMALAYAS
                )
        ).thenReturn(
                List.of(job)
        );

        when(
                hybridService.extract(
                        job.getTitle(),
                        job.getDescription()
                )
        ).thenThrow(
                new RuntimeException(
                        "AI provider unavailable"
                )
        );

        ExperienceReprocessingService.ReprocessingResult result =
                reprocessingService.reprocessJobsBySource(
                        JobSource.HIMALAYAS
                );

        assertAll(
                () -> assertEquals(
                        1,
                        result.scanned()
                ),
                () -> assertEquals(
                        0,
                        result.updated()
                ),
                () -> assertEquals(
                        1,
                        result.failed()
                ),
                () -> assertEquals(
                        new BigDecimal("2"),
                        job.getMinimumExperience()
                ),
                () -> assertEquals(
                        ExperienceRequirementType.REQUIRED,
                        job.getExperienceRequirementType()
                ),
                () -> assertEquals(
                        "RULE",
                        job.getExperienceExtractionMethod()
                )
        );

        verify(
                repository,
                never()
        ).saveAll(
                anyList()
        );

        verify(
                repository,
                never()
        ).flush();
    }

    private JobListingEntity createJob(
            String title,
            String description,
            JobSource source
    ) {
        JobListingEntity job =
                new JobListingEntity();

        job.setTitle(title);
        job.setDescription(description);
        job.setSource(source);
        job.setActive(true);

        return job;
    }
}