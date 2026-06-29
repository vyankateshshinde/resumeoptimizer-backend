package com.vyankatesh.resumeoptimizer.resumebuilder.dto;

import java.time.LocalDateTime;

public class ResumeBuilderHistoryResponse {

    private Long id;
    private Long resumeId;
    private String templateName;
    private String jobDescription;
    private LocalDateTime createdAt;
    private ResumeBuilderResponse resume;

    public ResumeBuilderHistoryResponse() {
    }

    public ResumeBuilderHistoryResponse(
            Long id,
            Long resumeId,
            String templateName,
            String jobDescription,
            LocalDateTime createdAt,
            ResumeBuilderResponse resume
    ) {
        this.id = id;
        this.resumeId = resumeId;
        this.templateName = templateName;
        this.jobDescription = jobDescription;
        this.createdAt = createdAt;
        this.resume = resume;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ResumeBuilderResponse getResume() {
        return resume;
    }

    public void setResume(ResumeBuilderResponse resume) {
        this.resume = resume;
    }
}
