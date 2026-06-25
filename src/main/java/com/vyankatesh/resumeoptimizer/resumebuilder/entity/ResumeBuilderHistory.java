package com.vyankatesh.resumeoptimizer.resumebuilder.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume_builder_history")
public class ResumeBuilderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long resumeId;
    private String userEmail;
    private String templateName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String jobDescription;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String generatedResumeJson;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
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

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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

    public String getGeneratedResumeJson() {
        return generatedResumeJson;
    }

    public void setGeneratedResumeJson(String generatedResumeJson) {
        this.generatedResumeJson = generatedResumeJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}