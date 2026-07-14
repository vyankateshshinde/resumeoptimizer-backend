package com.vyankatesh.resumeoptimizer.jobfinder.dto.response;

import java.time.LocalDateTime;

public record JobNotificationResponse(
        Long id,
        Long alertId,
        Long jobId,
        String title,
        String company,
        String location,
        String applyUrl,
        int matchPercentage,
        boolean read,
        LocalDateTime createdAt
) {
}
