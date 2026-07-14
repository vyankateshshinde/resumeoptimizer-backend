package com.vyankatesh.resumeoptimizer.jobfinder.dto.response;

import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSource;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record JobMatchResponse(
        Long jobId,
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
        LocalDateTime postedAt,
        JobSource source,
        String sourceName,
        String applyUrl,
        String descriptionPreview,
        int matchPercentage,
        int resumeMatchPercentage,
        int titleMatchPercentage,
        int experienceMatchPercentage,
        int freshnessPercentage,
        List<String> matchedSkills,
        List<String> missingSkills,
        String matchExplanation
) {
}
