package com.vyankatesh.resumeoptimizer.prompteditor.service;

import com.vyankatesh.resumeoptimizer.prompteditor.enums.PromptIntent;
import org.springframework.stereotype.Service;

@Service
public class IntentDetectionService {

    public PromptIntent detectIntent(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            return PromptIntent.CUSTOM;
        }

        String lowerPrompt = prompt.toLowerCase();

        // REMOVE SKILL
        if (lowerPrompt.contains("remove")
                || lowerPrompt.contains("don't know")
                || lowerPrompt.contains("do not know")
                || lowerPrompt.contains("not familiar")
                || lowerPrompt.contains("exclude")) {
            return PromptIntent.REMOVE_SKILL;
        }

        // ADD SKILL
        if (lowerPrompt.contains("add")
                && (lowerPrompt.contains("skill")
                || lowerPrompt.contains("technology")
                || lowerPrompt.contains("tool")
                || lowerPrompt.contains("keyword"))) {
            return PromptIntent.ADD_SKILL;
        }

        // REMOVE WEAK LINES
        if (lowerPrompt.contains("weak line")
                || lowerPrompt.contains("generic line")
                || lowerPrompt.contains("remove weak")
                || lowerPrompt.contains("remove generic")) {
            return PromptIntent.REMOVE_WEAK_LINES;
        }

        // IMPROVE SUMMARY
        if (lowerPrompt.contains("summary")
                || lowerPrompt.contains("profile summary")
                || lowerPrompt.contains("professional summary")) {
            return PromptIntent.IMPROVE_SUMMARY;
        }

        // IMPROVE EXPERIENCE
        if (lowerPrompt.contains("experience")
                || lowerPrompt.contains("work experience")) {
            return PromptIntent.IMPROVE_EXPERIENCE;
        }

        // IMPROVE PROJECTS
        if (lowerPrompt.contains("project")
                || lowerPrompt.contains("projects")) {
            return PromptIntent.IMPROVE_PROJECTS;
        }

        // ATS OPTIMIZATION
        if (lowerPrompt.contains("ats")
                || lowerPrompt.contains("keyword")
                || lowerPrompt.contains("optimize")
                || lowerPrompt.contains("job description")
                || lowerPrompt.contains("jd")) {
            return PromptIntent.ATS_OPTIMIZATION;
        }

        // ADD METRICS
        if (lowerPrompt.contains("metric")
                || lowerPrompt.contains("metrics")
                || lowerPrompt.contains("impact")
                || lowerPrompt.contains("achievement")
                || lowerPrompt.contains("quantify")) {
            return PromptIntent.ADD_METRICS;
        }

        // MAKE CONCISE
        if (lowerPrompt.contains("short")
                || lowerPrompt.contains("concise")
                || lowerPrompt.contains("reduce")
                || lowerPrompt.contains("smaller")
                || lowerPrompt.contains("less words")) {
            return PromptIntent.MAKE_CONCISE;
        }

        return PromptIntent.CUSTOM;
    }
}