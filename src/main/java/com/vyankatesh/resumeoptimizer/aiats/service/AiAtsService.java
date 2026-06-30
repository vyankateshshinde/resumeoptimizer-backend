package com.vyankatesh.resumeoptimizer.aiats.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyankatesh.resumeoptimizer.ai.service.GroqService;
import com.vyankatesh.resumeoptimizer.aiats.dto.AiAtsRequest;
import com.vyankatesh.resumeoptimizer.aiats.dto.AiAtsResponse;
import com.vyankatesh.resumeoptimizer.ats.entity.AtsHistoryEntity;
import com.vyankatesh.resumeoptimizer.ats.repository.AtsHistoryRepository;
import com.vyankatesh.resumeoptimizer.resume.ResumeEntity;
import com.vyankatesh.resumeoptimizer.resume.ResumeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiAtsService {

    private final ResumeRepository resumeRepository;
    private final GroqService groqService;
    private final ObjectMapper objectMapper;
    private final AtsHistoryRepository atsHistoryRepository;

    public AiAtsService(
            ResumeRepository resumeRepository,
            GroqService groqService,
            ObjectMapper objectMapper,
            AtsHistoryRepository atsHistoryRepository
    ) {
        this.resumeRepository = resumeRepository;
        this.groqService = groqService;
        this.objectMapper = objectMapper;
        this.atsHistoryRepository = atsHistoryRepository;
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

            AiAtsResponse response = objectMapper.readValue(aiJson, AiAtsResponse.class);

            List<String> matchedSkills = normalizeItems(response.getMatchedSkills());
            List<String> missingSkills = removeMatchedItems(
                    normalizeItems(response.getMissingSkills()),
                    matchedSkills
            );

            response.setMatchedSkills(matchedSkills);
            response.setMissingSkills(missingSkills);
            response.setStrengths(normalizeItems(response.getStrengths()));
            response.setWeaknesses(normalizeItems(response.getWeaknesses()));
            response.setRecommendations(normalizeItems(response.getRecommendations()));

            // Never trust a language model to calculate a numerical ATS percentage.
            // The score is calculated only from the final requirement lists returned by AI.
            response.setAtsScore(calculateAtsScore(matchedSkills, missingSkills));

            saveAiAtsHistory(resume, request.getJobDescription(), response);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI ATS response: " + e.getMessage(), e);
        }
    }

    private void saveAiAtsHistory(
            ResumeEntity resume,
            String jobDescription,
            AiAtsResponse response
    ) {
        AtsHistoryEntity history = new AtsHistoryEntity();

        history.setEmail(resume.getEmail());
        history.setResumeId(resume.getId());
        history.setJobDescription(normalizeJobDescription(jobDescription));
        history.setSkillScore(response.getAtsScore());
        history.setKeywordScore(response.getAtsScore());
        history.setFinalScore(response.getAtsScore());
        history.setMatchedSkills(String.join(", ", response.getMatchedSkills()));
        history.setMissingSkills(String.join(", ", response.getMissingSkills()));
        history.setFeedback(String.join("\n", response.getRecommendations()));
        history.setCreatedAt(LocalDateTime.now());

        atsHistoryRepository.save(history);
    }

    private int calculateAtsScore(List<String> matchedSkills, List<String> missingSkills) {
        int matchedCount = matchedSkills == null ? 0 : matchedSkills.size();
        int missingCount = missingSkills == null ? 0 : missingSkills.size();
        int totalRequirements = matchedCount + missingCount;

        if (totalRequirements == 0) {
            return 0;
        }

        return (int) Math.round((matchedCount * 100.0) / totalRequirements);
    }

    private List<String> normalizeItems(List<String> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, String> uniqueItems = new LinkedHashMap<>();

        for (String item : items) {
            if (item == null || item.isBlank()) {
                continue;
            }

            String cleanedItem = item
                    .replaceAll("^[\\s•-]+", "")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (!cleanedItem.isBlank()) {
                uniqueItems.putIfAbsent(cleanedItem.toLowerCase(Locale.ROOT), cleanedItem);
            }
        }

        return new ArrayList<>(uniqueItems.values());
    }

    private List<String> removeMatchedItems(List<String> missingSkills, List<String> matchedSkills) {
        if (missingSkills.isEmpty() || matchedSkills.isEmpty()) {
            return missingSkills;
        }

        Map<String, Boolean> matchedLookup = new LinkedHashMap<>();
        for (String matchedSkill : matchedSkills) {
            matchedLookup.put(matchedSkill.toLowerCase(Locale.ROOT), true);
        }

        List<String> filteredMissingSkills = new ArrayList<>();
        for (String missingSkill : missingSkills) {
            if (!matchedLookup.containsKey(missingSkill.toLowerCase(Locale.ROOT))) {
                filteredMissingSkills.add(missingSkill);
            }
        }

        return filteredMissingSkills;
    }

    private String normalizeJobDescription(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
