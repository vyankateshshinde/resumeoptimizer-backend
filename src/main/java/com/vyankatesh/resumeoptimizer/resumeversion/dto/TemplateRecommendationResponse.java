package com.vyankatesh.resumeoptimizer.resumeversion.dto;

import com.vyankatesh.resumeoptimizer.resumeversion.entity.ResumeTemplate;

public class TemplateRecommendationResponse {

    private String recommendedTemplateName;
    private String reason;
    private ResumeTemplate template;

    public TemplateRecommendationResponse() {
    }

    public TemplateRecommendationResponse(String recommendedTemplateName, String reason, ResumeTemplate template) {
        this.recommendedTemplateName = recommendedTemplateName;
        this.reason = reason;
        this.template = template;
    }

    public String getRecommendedTemplateName() {
        return recommendedTemplateName;
    }

    public void setRecommendedTemplateName(String recommendedTemplateName) {
        this.recommendedTemplateName = recommendedTemplateName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ResumeTemplate getTemplate() {
        return template;
    }

    public void setTemplate(ResumeTemplate template) {
        this.template = template;
    }
}