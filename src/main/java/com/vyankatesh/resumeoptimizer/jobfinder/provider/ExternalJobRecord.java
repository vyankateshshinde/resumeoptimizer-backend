package com.vyankatesh.resumeoptimizer.jobfinder.provider;

import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSource;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExternalJobRecord(
        String externalId,
        String title,
        String company,
        String location,
        WorkArrangement workArrangement,
        EmploymentType employmentType,
        BigDecimal minimumExperience,
        BigDecimal maximumExperience,
        BigDecimal minimumSalary,
        BigDecimal maximumSalary,
        String salaryCurrency,
        String description,
        LocalDateTime postedAt,
        JobSource source,
        String sourceName,
        String applyUrl
) {
}
