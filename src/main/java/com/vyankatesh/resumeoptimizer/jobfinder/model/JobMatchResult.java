package com.vyankatesh.resumeoptimizer.jobfinder.model;

import java.util.List;

public record JobMatchResult(
        int overallScore,
        int resumeScore,
        int titleScore,
        int experienceScore,
        int freshnessScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        String explanation
) {
}
