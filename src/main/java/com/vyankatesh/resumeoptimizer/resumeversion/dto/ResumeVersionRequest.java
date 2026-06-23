package com.vyankatesh.resumeoptimizer.resumeversion.dto;

public class ResumeVersionRequest {

    private Long resumeId;
    private String versionName;
    private String templateName;
    private String fullResumeText;
    private String professionalSummary;
    private String skills;
    private String experienceBullets;
    private String projectBullets;
    private String education;
    private int atsScore;

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

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

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getExperienceBullets() {
        return experienceBullets;
    }

    public void setExperienceBullets(String experienceBullets) {
        this.experienceBullets = experienceBullets;
    }

    public String getProjectBullets() {
        return projectBullets;
    }

    public void setProjectBullets(String projectBullets) {
        this.projectBullets = projectBullets;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public int getAtsScore() {
        return atsScore;
    }

    public void setAtsScore(int atsScore) {
        this.atsScore = atsScore;
    }
}