package com.vyankatesh.resumeoptimizer.prompteditor.dto;

public class PromptEditRequest {

    private String currentResumeText;
    private String jobDescription;
    private String userPrompt;

    public PromptEditRequest() {
    }

    public String getCurrentResumeText() {
        return currentResumeText;
    }

    public void setCurrentResumeText(String currentResumeText) {
        this.currentResumeText = currentResumeText;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }
}