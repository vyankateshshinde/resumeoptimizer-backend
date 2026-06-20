package com.vyankatesh.resumeoptimizer.resumeversion.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "resume_templates")
public class ResumeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String templateName;
    private String templateType;

    @Column(length = 1000)
    private String description;

    private String previewImageUrl;

    private boolean atsFriendly;
    private boolean active;
    private int marketFitScore;

    public ResumeTemplate() {
    }

    public ResumeTemplate(String templateName, String templateType, String description,
                          String previewImageUrl, boolean atsFriendly, boolean active,
                          int marketFitScore) {
        this.templateName = templateName;
        this.templateType = templateType;
        this.description = description;
        this.previewImageUrl = previewImageUrl;
        this.atsFriendly = atsFriendly;
        this.active = active;
        this.marketFitScore = marketFitScore;
    }

    public Long getId() {
        return id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPreviewImageUrl() {
        return previewImageUrl;
    }

    public void setPreviewImageUrl(String previewImageUrl) {
        this.previewImageUrl = previewImageUrl;
    }

    public boolean isAtsFriendly() {
        return atsFriendly;
    }

    public void setAtsFriendly(boolean atsFriendly) {
        this.atsFriendly = atsFriendly;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getMarketFitScore() {
        return marketFitScore;
    }

    public void setMarketFitScore(int marketFitScore) {
        this.marketFitScore = marketFitScore;
    }
}