package com.vyankatesh.resumeoptimizer.ats.service;

import com.vyankatesh.resumeoptimizer.ats.dto.AtsHistoryResponse;
import com.vyankatesh.resumeoptimizer.ats.dto.AtsResponse;
import com.vyankatesh.resumeoptimizer.ats.entity.AtsHistoryEntity;
import com.vyankatesh.resumeoptimizer.ats.repository.AtsHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AtsService {

    private final AtsHistoryRepository atsHistoryRepository;

    public AtsService(AtsHistoryRepository atsHistoryRepository) {
        this.atsHistoryRepository = atsHistoryRepository;
    }

    private static final Map<String, String> SKILL_CATEGORY = Map.ofEntries(
            Map.entry("java", "backend"),
            Map.entry("spring", "backend"),
            Map.entry("spring boot", "backend"),
            Map.entry("spring security", "backend"),
            Map.entry("spring mvc", "backend"),
            Map.entry("hibernate", "backend"),
            Map.entry("jpa", "backend"),
            Map.entry("microservices", "backend"),
            Map.entry("rest api", "backend"),
            Map.entry("rest apis", "backend"),
            Map.entry("restful api", "backend"),
            Map.entry("restful apis", "backend"),
            Map.entry("jwt", "backend"),
            Map.entry("kafka", "backend"),
            Map.entry("websocket", "backend"),
            Map.entry("stomp", "backend"),
            Map.entry("redis", "backend"),
            Map.entry("system design", "backend"),

            Map.entry("react", "frontend"),
            Map.entry("react.js", "frontend"),
            Map.entry("javascript", "frontend"),
            Map.entry("html", "frontend"),
            Map.entry("html5", "frontend"),
            Map.entry("css", "frontend"),
            Map.entry("css3", "frontend"),

            Map.entry("mysql", "database"),
            Map.entry("mongodb", "database"),
            Map.entry("oracle", "database"),

            Map.entry("docker", "devops"),
            Map.entry("kubernetes", "devops"),
            Map.entry("aws", "devops"),
            Map.entry("jenkins", "devops"),
            Map.entry("ci/cd", "devops"),

            Map.entry("git", "tools"),
            Map.entry("github", "tools"),
            Map.entry("maven", "tools"),
            Map.entry("postman", "tools"),
            Map.entry("swagger", "tools"),
            Map.entry("junit", "tools"),
            Map.entry("mockito", "tools")
    );

    public AtsResponse calculateAtsScore(
            String resumeText,
            String jobDescription,
            String email,
            Long resumeId
    ) {
        String resume = normalize(resumeText);
        String jd = normalize(jobDescription);

        Map<String, List<String>> matched = initializeSkillMap();
        Map<String, List<String>> missing = initializeSkillMap();

        int total = 0;
        int score = 0;

        for (Map.Entry<String, String> entry : SKILL_CATEGORY.entrySet()) {
            String skill = entry.getKey();
            String category = entry.getValue();

            if (containsSkill(jd, skill)) {
                total += 10;

                if (containsSkill(resume, skill)) {
                    matched.get(category).add(formatSkill(skill));
                    score += 10;
                } else {
                    missing.get(category).add(formatSkill(skill));
                }
            }
        }

        int skillScore = total == 0 ? 0 : (score * 100) / total;
        int keywordScore = calculateKeywordScore(resume, jd);
        int finalScore = (int) ((0.7 * skillScore) + (0.3 * keywordScore));

        List<String> matchedSkills = flatten(matched);
        List<String> missingSkills = flatten(missing);

        String feedback = generateSmartFeedback(finalScore, missingSkills);

        AtsResponse response = new AtsResponse();
        response.setSkillScore(skillScore);
        response.setKeywordScore(keywordScore);
        response.setFinalScore(finalScore);
        response.setMatchedSkills(matchedSkills);
        response.setMissingSkills(missingSkills);
        response.setFeedback(feedback);

        AtsHistoryEntity history = new AtsHistoryEntity();
        history.setEmail(email);
        history.setResumeId(resumeId);
        history.setJobDescription(jobDescription);
        history.setSkillScore(skillScore);
        history.setKeywordScore(keywordScore);
        history.setFinalScore(finalScore);
        history.setMatchedSkills(String.join(", ", matchedSkills));
        history.setMissingSkills(String.join(", ", missingSkills));
        history.setFeedback(feedback);
        history.setCreatedAt(LocalDateTime.now());

        atsHistoryRepository.save(history);

        return response;
    }

    public List<AtsHistoryResponse> getHistory(String email) {
        List<AtsHistoryEntity> historyList =
                atsHistoryRepository.findByEmailOrderByCreatedAtDesc(email);

        return historyList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AtsHistoryResponse getLatestHistory(String email, Long resumeId) {
        AtsHistoryEntity history = atsHistoryRepository
                .findTopByEmailAndResumeIdOrderByCreatedAtDesc(email, resumeId)
                .orElseThrow(() -> new RuntimeException("No ATS history found"));

        return mapToResponse(history);
    }

    private AtsHistoryResponse mapToResponse(AtsHistoryEntity history) {
        AtsHistoryResponse response = new AtsHistoryResponse();

        response.setId(history.getId());
        response.setResumeId(history.getResumeId());
        response.setJobDescription(history.getJobDescription());
        response.setSkillScore(history.getSkillScore());
        response.setKeywordScore(history.getKeywordScore());
        response.setFinalScore(history.getFinalScore());
        response.setMatchedSkills(history.getMatchedSkills());
        response.setMissingSkills(history.getMissingSkills());
        response.setFeedback(history.getFeedback());
        response.setCreatedAt(history.getCreatedAt());

        return response;
    }

    private Map<String, List<String>> initializeSkillMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("backend", new ArrayList<>());
        map.put("frontend", new ArrayList<>());
        map.put("database", new ArrayList<>());
        map.put("devops", new ArrayList<>());
        map.put("tools", new ArrayList<>());
        return map;
    }

    private String normalize(String text) {
        if (text == null) return "";

        return text.toLowerCase()
                .replace("reactjs", "react")
                .replace("react.js", "react")
                .replace("restful apis", "rest api")
                .replace("restful api", "rest api")
                .replace("rest apis", "rest api")
                .replaceAll("[^a-z0-9+#/\\.\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsSkill(String text, String skill) {
        String normalizedSkill = normalize(skill);
        return text.contains(normalizedSkill);
    }

    private String formatSkill(String skill) {
        return switch (skill) {
            case "spring boot" -> "Spring Boot";
            case "spring security" -> "Spring Security";
            case "spring mvc" -> "Spring MVC";
            case "rest api", "rest apis", "restful api", "restful apis" -> "REST APIs";
            case "react", "react.js" -> "React.js";
            case "html", "html5" -> "HTML5";
            case "css", "css3" -> "CSS3";
            case "ci/cd" -> "CI/CD";
            case "jpa" -> "JPA";
            case "jwt" -> "JWT";
            case "aws" -> "AWS";
            case "mysql" -> "MySQL";
            case "mongodb" -> "MongoDB";
            case "junit" -> "JUnit";
            default -> Arrays.stream(skill.split(" "))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                    .collect(Collectors.joining(" "));
        };
    }

    private int calculateKeywordScore(String resume, String jd) {
        Set<String> jdWords = Arrays.stream(jd.split("\\s+"))
                .filter(word -> word.length() > 3)
                .collect(Collectors.toSet());

        if (jdWords.isEmpty()) {
            return 0;
        }

        int match = 0;

        for (String word : jdWords) {
            if (resume.contains(word)) {
                match++;
            }
        }

        return (match * 100) / jdWords.size();
    }

    private List<String> flatten(Map<String, List<String>> map) {
        return map.values()
                .stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    private String generateSmartFeedback(int score, List<String> missingSkills) {
        if (score >= 85) {
            return missingSkills.isEmpty()
                    ? "Excellent match. You are highly suitable for this role."
                    : "Excellent match. Minor improvements needed: " + missingSkills;
        }

        if (score >= 70) {
            return missingSkills.isEmpty()
                    ? "Good match. Resume is well aligned with this job description."
                    : "Good match. Improve these skills or keywords: " + missingSkills;
        }

        if (score >= 50) {
            return missingSkills.isEmpty()
                    ? "Average match. Improve resume keywords and project alignment."
                    : "Average match. Focus on: " + missingSkills;
        }

        return missingSkills.isEmpty()
                ? "Low match. Resume needs stronger alignment with this job description."
                : "Low match. Strongly recommend improving: " + missingSkills;
    }
}