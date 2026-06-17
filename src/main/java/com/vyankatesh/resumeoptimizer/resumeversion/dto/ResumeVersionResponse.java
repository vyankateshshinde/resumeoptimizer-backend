package com.vyankatesh.resumeoptimizer.resumeversion.dto;

import java.time.LocalDateTime;

public class ResumeVersionResponse {

    private Long id;
    private Long resumeId;
    private String versionName;
    private String summary;
    private String skills;
    private String projects;
    private LocalDateTime createdAt;

    public ResumeVersionResponse(
            Long id,
            Long resumeId,
            String versionName,
            String summary,
            String skills,
            String projects,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.resumeId = resumeId;
        this.versionName = versionName;
        this.summary = summary;
        this.skills = skills;
        this.projects = projects;
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

    public String getSummary() {
        return summary;
    }

    public String getSkills() {
        return skills;
    }

    public String getProjects() {
        return projects;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
