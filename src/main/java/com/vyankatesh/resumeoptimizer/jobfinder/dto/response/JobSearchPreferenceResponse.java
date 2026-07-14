package com.vyankatesh.resumeoptimizer.jobfinder.dto.response;

import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSortOption;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record JobSearchPreferenceResponse(
        Long id,
        String name,
        Long resumeId,
        List<String> jobTitles,
        List<String> locations,
        Set<WorkArrangement> workArrangements,
        Set<EmploymentType> employmentTypes,
        BigDecimal experienceYears,
        Integer postedWithinDays,
        BigDecimal minimumSalary,
        Integer minimumMatchPercentage,
        JobSortOption sortBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
