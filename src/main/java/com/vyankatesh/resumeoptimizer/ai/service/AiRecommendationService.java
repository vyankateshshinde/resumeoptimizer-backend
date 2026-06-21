package com.vyankatesh.resumeoptimizer.ai.service;

import com.vyankatesh.resumeoptimizer.ai.dto.AiRecommendationRequest;
import com.vyankatesh.resumeoptimizer.ai.dto.AiRecommendationResponse;
import com.vyankatesh.resumeoptimizer.ai.entity.AiRecommendationEntity;
import com.vyankatesh.resumeoptimizer.ai.repository.AiRecommendationRepository;
import com.vyankatesh.resumeoptimizer.resume.ResumeEntity;
import com.vyankatesh.resumeoptimizer.resume.ResumeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiRecommendationService {

    private final AiRecommendationRepository aiRecommendationRepository;
    private final ResumeRepository resumeRepository;
    private final GroqService groqService;

    public AiRecommendationService(
            AiRecommendationRepository aiRecommendationRepository,
            ResumeRepository resumeRepository,
            GroqService groqService
    ) {
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.resumeRepository = resumeRepository;
        this.groqService = groqService;
    }

    public AiRecommendationResponse generateRecommendation(AiRecommendationRequest request) {

        if (request.getResumeId() == null) {
            throw new RuntimeException("Resume ID is required. Please run ATS analysis again.");
        }

        ResumeEntity resume = resumeRepository.findById(request.getResumeId())
                .orElseThrow(() -> new RuntimeException(
                        "Resume not found with id: " + request.getResumeId()
                                + ". Please select a valid uploaded resume and run ATS analysis again."
                ));

        String resumeText = resume.getExtractedText();

        if (resumeText == null || resumeText.isBlank()) {
            throw new RuntimeException("Resume extracted text is empty. Please upload and parse resume again.");
        }

        String jobDescription = request.getJobDescription();

        if (jobDescription == null || jobDescription.isBlank()) {
            throw new RuntimeException("Job description is required.");
        }

        String aiResponse = groqService.generateRecommendation(
                resumeText,
                jobDescription
        );

        String summary = extractSection(aiResponse, "Summary Recommendation:", "Skill Recommendation:");
        String skills = extractSection(aiResponse, "Skill Recommendation:", "Project Recommendation:");
        String projects = extractSection(aiResponse, "Project Recommendation:", "Missing Skills:");
        String missingSkills = extractSection(aiResponse, "Missing Skills:", "Learning Roadmap:");
        String roadmap = extractSection(aiResponse, "Learning Roadmap:", null);

        AiRecommendationEntity entity = new AiRecommendationEntity();
        entity.setResumeId(request.getResumeId());
        entity.setJobDescription(jobDescription);
        entity.setSummaryRecommendation(summary);
        entity.setSkillRecommendation(skills);
        entity.setProjectRecommendation(projects);
        entity.setMissingSkills(missingSkills);
        entity.setLearningRoadmap(roadmap);

        aiRecommendationRepository.save(entity);

        AiRecommendationResponse response = new AiRecommendationResponse();
        response.setSummaryRecommendation(summary);
        response.setSkillRecommendation(skills);
        response.setProjectRecommendation(projects);
        response.setMissingSkills(missingSkills);
        response.setLearningRoadmap(roadmap);

        return response;
    }

    public List<AiRecommendationEntity> getRecommendationHistory(Long resumeId) {

        if (resumeId == null) {
            throw new RuntimeException("Resume ID is required.");
        }

        return aiRecommendationRepository.findByResumeId(resumeId);
    }

    private String extractSection(String text, String startLabel, String endLabel) {

        if (text == null || text.isBlank()) {
            return "";
        }

        int startIndex = text.indexOf(startLabel);

        if (startIndex == -1) {
            return "";
        }

        startIndex = startIndex + startLabel.length();

        int endIndex;

        if (endLabel == null) {
            endIndex = text.length();
        } else {
            endIndex = text.indexOf(endLabel, startIndex);

            if (endIndex == -1) {
                endIndex = text.length();
            }
        }

        return text.substring(startIndex, endIndex).trim();
    }
}