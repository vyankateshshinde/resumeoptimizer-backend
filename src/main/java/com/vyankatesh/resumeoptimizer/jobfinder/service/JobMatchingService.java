package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobMatchResult;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSearchCriteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobMatchingService {

    private static final Set<String> KNOWN_SKILLS = new LinkedHashSet<>(List.of(
            "java", "spring boot", "spring security", "spring mvc", "hibernate", "jpa",
            "microservices", "rest api", "graphql", "kafka", "rabbitmq", "redis",
            "websocket", "mysql", "postgresql", "mongodb", "oracle", "sql",
            "react", "javascript", "typescript", "html", "css", "tailwind",
            "node.js", "express.js", "angular", "vue.js", "docker", "kubernetes",
            "aws", "azure", "gcp", "jenkins", "github actions", "ci/cd", "terraform",
            "linux", "git", "github", "maven", "gradle", "postman", "swagger",
            "junit", "mockito", "jest", "python", "django", "flask", "c++", "c#",
            ".net", "system design", "data structures", "algorithms", "agile", "scrum"
    ));

    private static final Set<String> STOP_WORDS = Set.of(
            "with", "from", "that", "this", "will", "your", "have", "has", "are",
            "the", "and", "for", "you", "our", "their", "into", "using", "work",
            "role", "job", "team", "years", "year", "experience", "required", "preferred",
            "responsibilities", "skills", "candidate", "strong", "good", "knowledge"
    );

    public JobMatchResult calculateMatch(
            String resumeText,
            JobSearchCriteria criteria,
            JobListingEntity job
    ) {
        String normalizedResume = normalize(resumeText);
        String normalizedJobText = normalize(job.getTitle() + " " + job.getDescription());

        Set<String> resumeSkills = extractKnownSkills(normalizedResume);
        Set<String> jobSkills = extractKnownSkills(normalizedJobText);

        List<String> matchedSkills = jobSkills.stream()
                .filter(resumeSkills::contains)
                .map(this::formatSkill)
                .toList();

        List<String> missingSkills = jobSkills.stream()
                .filter(skill -> !resumeSkills.contains(skill))
                .map(this::formatSkill)
                .toList();

        int resumeScore = calculateResumeScore(
                normalizedResume,
                normalizedJobText,
                matchedSkills.size(),
                jobSkills.size()
        );
        int titleScore = calculateTitleScore(criteria.jobTitles(), job.getTitle());
        int experienceScore = calculateExperienceScore(
                criteria.experienceYears(),
                job.getMinimumExperience(),
                job.getMaximumExperience()
        );
        int freshnessScore = calculateFreshnessScore(job.getPostedAt());

        int overallScore = clamp((int) Math.round(
                (resumeScore * 0.55)
                        + (titleScore * 0.20)
                        + (experienceScore * 0.15)
                        + (freshnessScore * 0.10)
        ));

        String explanation = buildExplanation(
                overallScore,
                resumeScore,
                titleScore,
                experienceScore,
                freshnessScore,
                missingSkills
        );

        return new JobMatchResult(
                overallScore,
                resumeScore,
                titleScore,
                experienceScore,
                freshnessScore,
                matchedSkills,
                missingSkills,
                explanation
        );
    }

    private int calculateResumeScore(
            String resumeText,
            String jobText,
            int matchedSkillCount,
            int totalJobSkillCount
    ) {
        int skillScore = totalJobSkillCount == 0
                ? 0
                : (matchedSkillCount * 100) / totalJobSkillCount;

        Set<String> resumeTokens = importantTokens(resumeText);
        Set<String> jobTokens = importantTokens(jobText);

        int keywordScore = 0;
        if (!jobTokens.isEmpty()) {
            long matches = jobTokens.stream().filter(resumeTokens::contains).count();
            keywordScore = (int) ((matches * 100) / jobTokens.size());
        }

        if (totalJobSkillCount == 0) {
            return clamp(keywordScore);
        }

        return clamp((int) Math.round((skillScore * 0.75) + (keywordScore * 0.25)));
    }

    private int calculateTitleScore(List<String> desiredTitles, String jobTitle) {
        String normalizedJobTitle = normalize(jobTitle);

        return desiredTitles.stream()
                .map(this::normalize)
                .mapToInt(desiredTitle -> {
                    if (normalizedJobTitle.equals(desiredTitle)) {
                        return 100;
                    }
                    if (normalizedJobTitle.contains(desiredTitle)
                            || desiredTitle.contains(normalizedJobTitle)) {
                        return 92;
                    }

                    Set<String> desiredTokens = importantTokens(desiredTitle);
                    Set<String> jobTokens = importantTokens(normalizedJobTitle);

                    if (desiredTokens.isEmpty() || jobTokens.isEmpty()) {
                        return 0;
                    }

                    Set<String> intersection = new LinkedHashSet<>(desiredTokens);
                    intersection.retainAll(jobTokens);

                    Set<String> union = new LinkedHashSet<>(desiredTokens);
                    union.addAll(jobTokens);

                    return clamp((intersection.size() * 100) / union.size());
                })
                .max()
                .orElse(0);
    }

    private int calculateExperienceScore(
            BigDecimal candidateExperience,
            BigDecimal minimumExperience,
            BigDecimal maximumExperience
    ) {
        if (candidateExperience == null) {
            return 70;
        }

        if (minimumExperience == null && maximumExperience == null) {
            return 75;
        }

        if (minimumExperience != null
                && candidateExperience.compareTo(minimumExperience) < 0) {
            BigDecimal gap = minimumExperience.subtract(candidateExperience);
            int deduction = (int) Math.round(
                    gap.multiply(BigDecimal.valueOf(25)).doubleValue()
            );
            return clamp(100 - deduction);
        }

        if (maximumExperience != null
                && candidateExperience.compareTo(maximumExperience) > 0) {
            BigDecimal gap = candidateExperience.subtract(maximumExperience);
            int deduction = (int) Math.round(
                    gap.multiply(BigDecimal.valueOf(10)).doubleValue()
            );
            return Math.max(40, clamp(100 - deduction));
        }

        return 100;
    }

    private int calculateFreshnessScore(LocalDateTime postedAt) {
        if (postedAt == null) {
            return 25;
        }

        long hours = Math.max(0, Duration.between(postedAt, LocalDateTime.now()).toHours());

        if (hours <= 24) return 100;
        if (hours <= 48) return 95;
        if (hours <= 72) return 90;
        if (hours <= 7 * 24L) return 75;
        if (hours <= 14 * 24L) return 50;
        if (hours <= 30 * 24L) return 30;
        return 10;
    }

    private Set<String> extractKnownSkills(String text) {
        return KNOWN_SKILLS.stream()
                .filter(skill -> containsPhrase(text, skill))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean containsPhrase(String text, String phrase) {
        String normalizedPhrase = normalize(phrase);
        return text.equals(normalizedPhrase)
                || text.startsWith(normalizedPhrase + " ")
                || text.endsWith(" " + normalizedPhrase)
                || text.contains(" " + normalizedPhrase + " ");
    }

    private Set<String> importantTokens(String text) {
        return Arrays.stream(normalize(text).split("\\s+"))
                .filter(token -> token.length() > 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.toLowerCase(Locale.ROOT)
                .replace("reactjs", "react")
                .replace("react.js", "react")
                .replace("nodejs", "node.js")
                .replace("springboot", "spring boot")
                .replace("restful api", "rest api")
                .replace("rest apis", "rest api")
                .replaceAll("[^a-z0-9+#./\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String formatSkill(String skill) {
        return switch (skill) {
            case "spring boot" -> "Spring Boot";
            case "spring security" -> "Spring Security";
            case "spring mvc" -> "Spring MVC";
            case "rest api" -> "REST APIs";
            case "node.js" -> "Node.js";
            case "express.js" -> "Express.js";
            case "ci/cd" -> "CI/CD";
            case "aws" -> "AWS";
            case "gcp" -> "GCP";
            case "sql" -> "SQL";
            case "mysql" -> "MySQL";
            case "mongodb" -> "MongoDB";
            case "postgresql" -> "PostgreSQL";
            case "javascript" -> "JavaScript";
            case "typescript" -> "TypeScript";
            case "jpa" -> "JPA";
            case "html" -> "HTML";
            case "css" -> "CSS";
            case "junit" -> "JUnit";
            default -> Arrays.stream(skill.split(" "))
                    .map(word -> word.isEmpty()
                            ? word
                            : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                    .collect(Collectors.joining(" "));
        };
    }

    private String buildExplanation(
            int overallScore,
            int resumeScore,
            int titleScore,
            int experienceScore,
            int freshnessScore,
            List<String> missingSkills
    ) {
        List<String> reasons = new ArrayList<>();

        if (resumeScore >= 75) reasons.add("strong resume-to-description alignment");
        if (titleScore >= 80) reasons.add("close desired-title match");
        if (experienceScore >= 85) reasons.add("compatible experience range");
        if (freshnessScore >= 90) reasons.add("recently posted");

        String level = overallScore >= 85
                ? "Excellent match"
                : overallScore >= 70
                ? "Good match"
                : overallScore >= 50
                ? "Moderate match"
                : "Low match";

        String reasonText = reasons.isEmpty()
                ? "based on the available resume, title, experience and posting-date signals"
                : "because of " + String.join(", ", reasons);

        if (missingSkills.isEmpty()) {
            return level + " " + reasonText + ".";
        }

        return level + " " + reasonText
                + ". Main gaps: "
                + String.join(", ", missingSkills.stream().limit(5).toList())
                + ".";
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}