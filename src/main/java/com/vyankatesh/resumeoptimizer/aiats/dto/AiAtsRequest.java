package com.vyankatesh.resumeoptimizer.aiats.dto;

public class AiAtsRequest {

    private Long resumeId;
    private String jobDescription;

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }
}