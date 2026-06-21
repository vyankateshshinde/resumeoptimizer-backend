package com.vyankatesh.resumeoptimizer.resumebuilder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyankatesh.resumeoptimizer.ai.service.GroqService;
import com.vyankatesh.resumeoptimizer.resume.ResumeEntity;
import com.vyankatesh.resumeoptimizer.resume.ResumeRepository;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBuilderRequest;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBuilderResponse;
import org.springframework.stereotype.Service;

@Service
public class ResumeBuilderService {

    private final ResumeRepository resumeRepository;
    private final GroqService groqService;
    private final ObjectMapper objectMapper;

    public ResumeBuilderService(
            ResumeRepository resumeRepository,
            GroqService groqService,
            ObjectMapper objectMapper
    ) {
        this.resumeRepository = resumeRepository;
        this.groqService = groqService;
        this.objectMapper = objectMapper;
    }

    public ResumeBuilderResponse generateResume(ResumeBuilderRequest request) {

        if (request.getResumeId() == null) {
            throw new RuntimeException("Resume ID is required");
        }

        if (request.getJobDescription() == null || request.getJobDescription().isBlank()) {
            throw new RuntimeException("Job description is required");
        }

        String templateName = request.getTemplateName();

        if (templateName == null || templateName.isBlank()) {
            templateName = "Modern ATS Template";
        }

        ResumeEntity resume = resumeRepository.findById(request.getResumeId())
                .orElseThrow(() -> new RuntimeException(
                        "Resume not found with id: " + request.getResumeId()
                ));

        String resumeText = resume.getExtractedText();

        if (resumeText == null || resumeText.isBlank()) {
            throw new RuntimeException("Resume extracted text is empty. Please upload resume again.");
        }

        try {
            String aiJson = groqService.generateResumeBuilderContent(
                    resumeText,
                    request.getJobDescription(),
                    templateName
            );

            return objectMapper.readValue(aiJson, ResumeBuilderResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate resume builder content: " + e.getMessage());
        }
    }
}