package com.vyankatesh.resumeoptimizer.jobfinder.dto.response;

import java.time.LocalDateTime;

public record JobAlertResponse(
        Long id,
        JobSearchPreferenceResponse preference,
        boolean enabled,
        LocalDateTime lastCheckedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
