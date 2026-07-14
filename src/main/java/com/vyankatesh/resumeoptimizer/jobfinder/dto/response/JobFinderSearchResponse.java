package com.vyankatesh.resumeoptimizer.jobfinder.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record JobFinderSearchResponse(
        long totalElements,
        int totalPages,
        int page,
        int size,
        LocalDateTime searchedAt,
        List<JobMatchResponse> jobs
) {
}
