package com.vyankatesh.resumeoptimizer.jobfinder.controller;

import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSource;
import com.vyankatesh.resumeoptimizer.jobfinder.service.ExperienceReprocessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/job-finder/admin")
public class ExperienceReprocessingController {

    private final ExperienceReprocessingService
            experienceReprocessingService;

    private final String administratorEmail;

    public ExperienceReprocessingController(
            ExperienceReprocessingService experienceReprocessingService,

            @Value("${jobfinder.admin.email:}")
            String administratorEmail
    ) {
        this.experienceReprocessingService =
                experienceReprocessingService;

        this.administratorEmail =
                administratorEmail == null
                        ? ""
                        : administratorEmail.trim();
    }

    @PostMapping("/reprocess-experience")
    public ExperienceReprocessingService.ReprocessingResult
    reprocessExperience(
            @RequestParam(required = false)
            String source,
            Authentication authentication
    ) {
        validateAdministrator(authentication);

        if (!StringUtils.hasText(source)
                || "ALL".equalsIgnoreCase(source.trim())) {

            return experienceReprocessingService
                    .reprocessAllJobs();
        }

        JobSource selectedSource =
                parseSource(source);

        return experienceReprocessingService
                .reprocessJobsBySource(
                        selectedSource
                );
    }

    private void validateAdministrator(
            Authentication authentication
    ) {
        if (!StringUtils.hasText(administratorEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Job Finder administrator email is not configured"
            );
        }

        if (authentication == null
                || !authentication.isAuthenticated()
                || !StringUtils.hasText(
                authentication.getName()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        String authenticatedEmail =
                authentication.getName().trim();

        if (!administratorEmail.equalsIgnoreCase(
                authenticatedEmail
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the Job Finder administrator can run reprocessing"
            );
        }
    }

    private JobSource parseSource(
            String source
    ) {
        try {
            return JobSource.valueOf(
                    source.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid job source. Allowed values: "
                            + allowedSources()
            );
        }
    }

    private String allowedSources() {
        return Arrays.stream(
                        JobSource.values()
                )
                .map(Enum::name)
                .collect(
                        Collectors.joining(", ")
                );
    }
}