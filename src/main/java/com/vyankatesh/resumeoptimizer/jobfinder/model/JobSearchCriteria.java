package com.vyankatesh.resumeoptimizer.jobfinder.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record JobSearchCriteria(
        List<String> jobTitles,
        List<String> locations,
        Set<WorkArrangement> workArrangements,
        Set<EmploymentType> employmentTypes,
        BigDecimal experienceYears,
        BigDecimal minimumSalary,
        LocalDateTime postedAfter,
        JobSortOption sortOption,
        int minimumMatchPercentage
) {
}
