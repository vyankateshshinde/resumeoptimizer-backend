package com.vyankatesh.resumeoptimizer.prompteditor.dto;

public class PromptEditResponse {

    private String updatedResumeText;
    private String detectedIntent;
    private String modifiedSection;
    private String changeSummary;

    public PromptEditResponse() {
    }

    public PromptEditResponse(String updatedResumeText, String detectedIntent,
                              String modifiedSection, String changeSummary) {
        this.updatedResumeText = updatedResumeText;
        this.detectedIntent = detectedIntent;
        this.modifiedSection = modifiedSection;
        this.changeSummary = changeSummary;
    }

    public String getUpdatedResumeText() {
        return updatedResumeText;
    }

    public void setUpdatedResumeText(String updatedResumeText) {
        this.updatedResumeText = updatedResumeText;
    }

    public String getDetectedIntent() {
        return detectedIntent;
    }

    public void setDetectedIntent(String detectedIntent) {
        this.detectedIntent = detectedIntent;
    }

    public String getModifiedSection() {
        return modifiedSection;
    }

    public void setModifiedSection(String modifiedSection) {
        this.modifiedSection = modifiedSection;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }
}