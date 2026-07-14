package com.vyankatesh.resumeoptimizer.jobfinder.dto.request;

import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSortOption;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record JobSearchPreferenceRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        Long resumeId,

        @NotEmpty
        @Size(max = 5)
        List<@NotBlank @Size(max = 120) String> jobTitles,

        @Size(max = 10)
        List<@NotBlank @Size(max = 120) String> locations,

        @Size(max = 4)
        Set<WorkArrangement> workArrangements,

        @Size(max = 6)
        Set<EmploymentType> employmentTypes,

        @DecimalMin("0.0")
        @DecimalMax("60.0")
        BigDecimal experienceYears,

        @Min(1)
        @Max(30)
        Integer postedWithinDays,

        @DecimalMin("0.0")
        BigDecimal minimumSalary,

        @Min(0)
        @Max(100)
        Integer minimumMatchPercentage,

        JobSortOption sortBy
) {
}
