package com.vyankatesh.resumeoptimizer.prompteditor.service;

import com.vyankatesh.resumeoptimizer.ai.service.GroqService;
import com.vyankatesh.resumeoptimizer.prompteditor.dto.PromptEditRequest;
import com.vyankatesh.resumeoptimizer.prompteditor.dto.PromptEditResponse;
import com.vyankatesh.resumeoptimizer.prompteditor.enums.PromptIntent;
import com.vyankatesh.resumeoptimizer.prompteditor.util.PromptBuilderUtil;
import org.springframework.stereotype.Service;

@Service
public class PromptResumeEditorService {

    private final IntentDetectionService intentDetectionService;
    private final PromptBuilderUtil promptBuilderUtil;
    private final GroqService groqService;

    public PromptResumeEditorService(
            IntentDetectionService intentDetectionService,
            PromptBuilderUtil promptBuilderUtil,
            GroqService groqService) {

        this.intentDetectionService = intentDetectionService;
        this.promptBuilderUtil = promptBuilderUtil;
        this.groqService = groqService;
    }

    public PromptEditResponse refineResume(PromptEditRequest request) {

        PromptIntent intent =
                intentDetectionService.detectIntent(request.getUserPrompt());

        String finalPrompt =
                promptBuilderUtil.buildPrompt(
                        intent,
                        request.getCurrentResumeText(),
                        request.getJobDescription(),
                        request.getUserPrompt()
                );

        String updatedResume =
                groqService.generatePromptBasedResumeEdit(
                        request.getCurrentResumeText(),
                        request.getJobDescription(),
                        finalPrompt
                );

        return new PromptEditResponse(
                updatedResume,
                intent.name(),
                detectModifiedSection(intent),
                generateChangeSummary(intent)
        );
    }

    private String detectModifiedSection(PromptIntent intent) {

        return switch (intent) {

            case REMOVE_SKILL,
                    ADD_SKILL -> "Skills";

            case IMPROVE_SUMMARY -> "Summary";

            case IMPROVE_EXPERIENCE -> "Experience";

            case IMPROVE_PROJECTS -> "Projects";

            case ATS_OPTIMIZATION -> "Entire Resume";

            case REMOVE_WEAK_LINES -> "Multiple Sections";

            case ADD_METRICS -> "Experience & Projects";

            case MAKE_CONCISE -> "Entire Resume";

            default -> "Custom";
        };
    }

    private String generateChangeSummary(PromptIntent intent) {

        return switch (intent) {

            case REMOVE_SKILL ->
                    "Removed specified skills and related references.";

            case ADD_SKILL ->
                    "Added requested skills where appropriate.";

            case REMOVE_WEAK_LINES ->
                    "Removed or improved weak resume content.";

            case IMPROVE_SUMMARY ->
                    "Enhanced professional summary.";

            case IMPROVE_EXPERIENCE ->
                    "Improved experience section with stronger impact.";

            case IMPROVE_PROJECTS ->
                    "Enhanced project descriptions.";

            case ATS_OPTIMIZATION ->
                    "Optimized resume according to job description.";

            case ADD_METRICS ->
                    "Added impact-driven achievements and metrics.";

            case MAKE_CONCISE ->
                    "Made resume more concise and focused.";

            default ->
                    "Applied custom resume modifications.";
        };
    }
}