package com.vyankatesh.resumeoptimizer.resumebuilder.dto;

import lombok.Data;

@Data
public class ResumeBuilderRefineRequest {

    private Long resumeId;
    private String jobDescription;
    private String userPrompt;
    private String templateName;
    private ResumeBuilderResponse resume;
}
