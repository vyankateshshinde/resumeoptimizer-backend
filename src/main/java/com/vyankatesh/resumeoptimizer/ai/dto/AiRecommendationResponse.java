package com.vyankatesh.resumeoptimizer.ai.dto;

public class AiRecommendationResponse {

    private String summaryRecommendation;
    private String skillRecommendation;
    private String projectRecommendation;
    private String missingSkills;
    private String learningRoadmap;

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
}