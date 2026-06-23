package com.vyankatesh.resumeoptimizer.resumeversion.dto;

import java.time.LocalDateTime;

public class ResumeVersionResponse {

    private Long id;
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
    private LocalDateTime createdAt;

    public ResumeVersionResponse() {
    }

    public ResumeVersionResponse(
            Long id,
            Long resumeId,
            String versionName,
            String templateName,
            String fullResumeText,
            String professionalSummary,
            String skills,
            String experienceBullets,
            String projectBullets,
            String education,
            int atsScore,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.resumeId = resumeId;
        this.versionName = versionName;
        this.templateName = templateName;
        this.fullResumeText = fullResumeText;
        this.professionalSummary = professionalSummary;
        this.skills = skills;
        this.experienceBullets = experienceBullets;
        this.projectBullets = projectBullets;
        this.education = education;
        this.atsScore = atsScore;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getFullResumeText() {
        return fullResumeText;
    }

    public String getProfessionalSummary() {
        return professionalSummary;
    }

    public String getSkills() {
        return skills;
    }

    public String getExperienceBullets() {
        return experienceBullets;
    }

    public String getProjectBullets() {
        return projectBullets;
    }

    public String getEducation() {
        return education;
    }

    public int getAtsScore() {
        return atsScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}