package com.vyankatesh.resumeoptimizer.aiats.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyankatesh.resumeoptimizer.ai.service.GroqService;
import com.vyankatesh.resumeoptimizer.aiats.dto.AiAtsRequest;
import com.vyankatesh.resumeoptimizer.aiats.dto.AiAtsResponse;
import com.vyankatesh.resumeoptimizer.resume.ResumeEntity;
import com.vyankatesh.resumeoptimizer.resume.ResumeRepository;
import org.springframework.stereotype.Service;

@Service
public class AiAtsService {

    private final ResumeRepository resumeRepository;
    private final GroqService groqService;
    private final ObjectMapper objectMapper;

    public AiAtsService(
            ResumeRepository resumeRepository,
            GroqService groqService,
            ObjectMapper objectMapper
    ) {
        this.resumeRepository = resumeRepository;
        this.groqService = groqService;
        this.objectMapper = objectMapper;
    }

    public AiAtsResponse analyzeResume(AiAtsRequest request) {

        if (request.getResumeId() == null) {
            throw new RuntimeException("Resume ID is required");
        }

        if (request.getJobDescription() == null || request.getJobDescription().isBlank()) {
            throw new RuntimeException("Job description is required");
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
            String aiJson = groqService.generateAiAtsAnalysis(
                    resumeText,
                    request.getJobDescription()
            );

            return objectMapper.readValue(aiJson, AiAtsResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI ATS response: " + e.getMessage());
        }
    }
}