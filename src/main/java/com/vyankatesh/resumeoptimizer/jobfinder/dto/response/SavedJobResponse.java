package com.vyankatesh.resumeoptimizer.jobfinder.dto.response;

import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;

import java.time.LocalDateTime;

public record SavedJobResponse(
        Long savedJobId,
        Long jobId,
        String title,
        String company,
        String location,
        WorkArrangement workArrangement,
        EmploymentType employmentType,
        LocalDateTime postedAt,
        String applyUrl,
        LocalDateTime savedAt
) {
}
