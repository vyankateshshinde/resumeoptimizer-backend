package com.vyankatesh.resumeoptimizer.resumebuilder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyankatesh.resumeoptimizer.ai.service.GroqService;
import com.vyankatesh.resumeoptimizer.resume.ResumeEntity;
import com.vyankatesh.resumeoptimizer.resume.ResumeRepository;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBuilderRequest;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBuilderResponse;
import com.vyankatesh.resumeoptimizer.resumebuilder.entity.ResumeBuilderHistory;
import com.vyankatesh.resumeoptimizer.resumebuilder.repository.ResumeBuilderHistoryRepository;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionRequest;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionResponse;
import com.vyankatesh.resumeoptimizer.resumeversion.service.ResumeVersionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

@Service
public class ResumeBuilderService {

    private final ResumeRepository resumeRepository;
    private final GroqService groqService;
    private final ObjectMapper objectMapper;
    private final ResumeBuilderHistoryRepository historyRepository;
    private final ResumeVersionService resumeVersionService;

    public ResumeBuilderService(
            ResumeRepository resumeRepository,
            GroqService groqService,
            ObjectMapper objectMapper,
            ResumeBuilderHistoryRepository historyRepository,
            ResumeVersionService resumeVersionService
    ) {
        this.resumeRepository = resumeRepository;
        this.groqService = groqService;
        this.objectMapper = objectMapper;
        this.historyRepository = historyRepository;
        this.resumeVersionService = resumeVersionService;
    }

    public ResumeBuilderResponse generateResume(
            ResumeBuilderRequest request,
            String userEmail
    ) {
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

            ResumeBuilderResponse response =
                    objectMapper.readValue(aiJson, ResumeBuilderResponse.class);

            ResumeBuilderHistory history = new ResumeBuilderHistory();
            history.setResumeId(request.getResumeId());
            history.setUserEmail(userEmail);
            history.setTemplateName(response.getTemplateName());
            history.setJobDescription(request.getJobDescription());
            history.setGeneratedResumeJson(aiJson);

            historyRepository.save(history);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate resume builder content: " + e.getMessage());
        }
    }

    public List<ResumeBuilderHistory> getHistory(String userEmail) {
        return historyRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    public ResumeVersionResponse saveHistoryAsVersion(Long historyId, String userEmail) {

        ResumeBuilderHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("Resume builder history not found with id: " + historyId));

        if (!history.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("You are not allowed to save this resume history");
        }

        try {
            ResumeBuilderResponse builderResponse = objectMapper.readValue(
                    history.getGeneratedResumeJson(),
                    ResumeBuilderResponse.class
            );

            ResumeVersionRequest versionRequest = new ResumeVersionRequest();

            versionRequest.setResumeId(history.getResumeId());
            versionRequest.setVersionName("AI Generated Resume - " + history.getTemplateName());
            versionRequest.setTemplateName(builderResponse.getTemplateName());
            versionRequest.setFullResumeText(builderResponse.getFullResumeText());
            versionRequest.setProfessionalSummary(builderResponse.getProfessionalSummary());
            versionRequest.setSkills(joinList(builderResponse.getSkills()));
            versionRequest.setExperienceBullets(joinList(builderResponse.getExperienceBullets()));
            versionRequest.setProjectBullets(joinList(builderResponse.getProjectBullets()));
            versionRequest.setEducation(builderResponse.getEducation());
            versionRequest.setAtsScore(0);

            return resumeVersionService.saveVersion(versionRequest, userEmail);

        } catch (Exception e) {
            throw new RuntimeException("Failed to save builder history as resume version: " + e.getMessage());
        }
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                joiner.add(value.trim());
            }
        }

        return joiner.toString();
    }
}