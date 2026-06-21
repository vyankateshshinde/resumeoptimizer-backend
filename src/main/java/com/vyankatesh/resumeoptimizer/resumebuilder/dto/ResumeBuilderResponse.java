package com.vyankatesh.resumeoptimizer.resumebuilder.dto;

import java.util.List;

public class ResumeBuilderResponse {

    private String templateName;
    private String fullResumeText;

    private String professionalSummary;
    private List<String> skills;
    private List<String> experienceBullets;
    private List<String> projectBullets;
    private String education;

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getFullResumeText() {
        return fullResumeText;
    }

    public void setFullResumeText(String fullResumeText) {
        this.fullResumeText = fullResumeText;
    }

    public String getProfessionalSummary() {
        return professionalSummary;
    }

    public void setProfessionalSummary(String professionalSummary) {
        this.professionalSummary = professionalSummary;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getExperienceBullets() {
        return experienceBullets;
    }

    public void setExperienceBullets(List<String> experienceBullets) {
        this.experienceBullets = experienceBullets;
    }

    public List<String> getProjectBullets() {
        return projectBullets;
    }

    public void setProjectBullets(List<String> projectBullets) {
        this.projectBullets = projectBullets;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }
}