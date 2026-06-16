package com.vyankatesh.resumeoptimizer.ats.service;
import com.vyankatesh.resumeoptimizer.ats.dto.AtsHistoryResponse;
import java.util.stream.Collectors;
import com.vyankatesh.resumeoptimizer.ats.dto.AtsResponse;
import com.vyankatesh.resumeoptimizer.ats.entity.AtsHistoryEntity;
import com.vyankatesh.resumeoptimizer.ats.repository.AtsHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AtsService {

    private final AtsHistoryRepository atsHistoryRepository;

    public AtsService(AtsHistoryRepository atsHistoryRepository) {
        this.atsHistoryRepository = atsHistoryRepository;
    }

    private static final Map<String, String> SKILL_CATEGORY = Map.ofEntries(

            Map.entry("java", "backend"),
            Map.entry("spring boot", "backend"),
            Map.entry("microservices", "backend"),

            Map.entry("kafka", "backend"),
            Map.entry("redis", "backend"),
            Map.entry("rest api", "backend"),
            Map.entry("system design", "backend"),

            Map.entry("mysql", "database"),
            Map.entry("mongodb", "database"),

            Map.entry("docker", "devops"),
            Map.entry("kubernetes", "devops"),
            Map.entry("aws", "devops"),
            Map.entry("jenkins", "devops"),

            Map.entry("git", "tools")
    );

    public AtsResponse calculateAtsScore(
            String resumeText,
            String jobDescription,
            String email,
            Long resumeId
    ) {

        String resume = resumeText.toLowerCase();
        String jd = jobDescription.toLowerCase();

        Map<String, List<String>> matched = new HashMap<>();
        Map<String, List<String>> missing = new HashMap<>();

        matched.put("backend", new ArrayList<>());
        matched.put("devops", new ArrayList<>());
        matched.put("database", new ArrayList<>());
        matched.put("tools", new ArrayList<>());

        missing.put("backend", new ArrayList<>());
        missing.put("devops", new ArrayList<>());
        missing.put("database", new ArrayList<>());
        missing.put("tools", new ArrayList<>());

        int total = 0;
        int score = 0;

        // =========================
        // SKILL MATCHING
        // =========================
        for (Map.Entry<String, String> entry : SKILL_CATEGORY.entrySet()) {

            String skill = entry.getKey();
            String category = entry.getValue();

            if (jd.contains(skill)) {

                total += 10;

                if (resume.contains(skill)) {
                    matched.get(category).add(skill);
                    score += 10;
                } else {
                    missing.get(category).add(skill);
                }
            }
        }

        int skillScore = total == 0
                ? 0
                : (score * 100) / total;

        int keywordScore =
                calculateKeywordScore(resume, jd);

        int finalScore =
                (int) ((0.7 * skillScore) + (0.3 * keywordScore));

        String feedback =
                generateSmartFeedback(finalScore, missing);

        // =========================
        // BUILD RESPONSE
        // =========================
        AtsResponse response = new AtsResponse();

        response.setSkillScore(skillScore);
        response.setKeywordScore(keywordScore);
        response.setFinalScore(finalScore);

        response.setMatchedSkills(
                flatten(matched)
        );

        response.setMissingSkills(
                flatten(missing)
        );

        response.setFeedback(feedback);

        // =========================
        // SAVE ATS HISTORY
        // =========================
        AtsHistoryEntity history = new AtsHistoryEntity();

        history.setEmail(email);
        history.setResumeId(resumeId);

        history.setJobDescription(jobDescription);

        history.setSkillScore(skillScore);
        history.setKeywordScore(keywordScore);
        history.setFinalScore(finalScore);

        history.setMatchedSkills(
                String.join(", ", flatten(matched))
        );

        history.setMissingSkills(
                String.join(", ", flatten(missing))
        );

        history.setFeedback(feedback);

        history.setCreatedAt(
                LocalDateTime.now()
        );

        atsHistoryRepository.save(history);

        return response;
    }

    // =========================
    // KEYWORD SCORE
    // =========================
    private int calculateKeywordScore(
            String resume,
            String jd
    ) {

        String[] jdWords = jd.split("\\s+");

        int match = 0;

        for (String word : jdWords) {

            if (word.length() > 2 &&
                    resume.contains(word)) {

                match++;
            }
        }

        return jdWords.length == 0
                ? 0
                : (match * 100) / jdWords.length;
    }

    // =========================
    // FLATTEN MAP
    // =========================
    private List<String> flatten(
            Map<String, List<String>> map
    ) {

        List<String> result = new ArrayList<>();

        for (List<String> list : map.values()) {
            result.addAll(list);
        }

        return result;
    }

    // =========================
    // FEEDBACK GENERATION
    // =========================
    private String generateSmartFeedback(
            int score,
            Map<String, List<String>> missing
    ) {

        List<String> allMissing =
                new ArrayList<>();

        missing.values()
                .forEach(allMissing::addAll);

        if (score >= 85) {
            return "Excellent match. You are highly suitable for this role.";
        }

        if (score >= 70) {
            return "Good match. Improve: " + allMissing;
        }

        if (score >= 50) {
            return "Average match. Focus on: " + allMissing;
        }

        return "Low match. Strongly recommend learning: " + allMissing;
    }
    public List<AtsHistoryResponse> getHistory(String email) {

        List<AtsHistoryEntity> historyList =
                atsHistoryRepository.findByEmailOrderByCreatedAtDesc(email);

        return historyList.stream()
                .map(history -> {

                    AtsHistoryResponse response =
                            new AtsHistoryResponse();

                    response.setId(history.getId());
                    response.setResumeId(history.getResumeId());

                    response.setSkillScore(history.getSkillScore());
                    response.setKeywordScore(history.getKeywordScore());
                    response.setFinalScore(history.getFinalScore());

                    response.setMatchedSkills(history.getMatchedSkills());
                    response.setMissingSkills(history.getMissingSkills());

                    response.setFeedback(history.getFeedback());

                    response.setCreatedAt(history.getCreatedAt());

                    return response;
                })
                .collect(Collectors.toList());
    }
}