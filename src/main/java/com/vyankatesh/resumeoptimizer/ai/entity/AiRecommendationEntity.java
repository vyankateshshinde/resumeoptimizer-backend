package com.vyankatesh.resumeoptimizer.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_recommendations")
public class AiRecommendationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long resumeId;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(columnDefinition = "TEXT")
    private String summaryRecommendation;

    @Column(columnDefinition = "TEXT")
    private String skillRecommendation;

    @Column(columnDefinition = "TEXT")
    private String projectRecommendation;

    @Column(columnDefinition = "TEXT")
    private String missingSkills;

    @Column(columnDefinition = "TEXT")
    private String learningRoadmap;

    private LocalDateTime createdAt;

    public AiRecommendationEntity() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getSummaryRecommendation() {
        return summaryRecommendation;
    }

    public void setSummaryRecommendation(String summaryRecommendation) {
        this.summaryRecommendation = summaryRecommendation;
    }

    public String getSkillRecommendation() {
        return skillRecommendation;
    }

    public void setSkillRecommendation(String skillRecommendation) {
        this.skillRecommendation = skillRecommendation;
    }

    public String getProjectRecommendation() {
        return projectRecommendation;
    }

    public void setProjectRecommendation(String projectRecommendation) {
        this.projectRecommendation = projectRecommendation;
    }

    public String getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(String missingSkills) {
        this.missingSkills = missingSkills;
    }

    public String getLearningRoadmap() {
        return learningRoadmap;
    }

    public void setLearningRoadmap(String learningRoadmap) {
        this.learningRoadmap = learningRoadmap;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}