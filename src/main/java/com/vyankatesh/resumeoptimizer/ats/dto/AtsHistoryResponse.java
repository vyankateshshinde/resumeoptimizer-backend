package com.vyankatesh.resumeoptimizer.ats.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AtsHistoryResponse {

    private Long id;

    private Long resumeId;

    private int skillScore;

    private int keywordScore;

    private int finalScore;

    private String matchedSkills;

    private String missingSkills;

    private String feedback;

    private LocalDateTime createdAt;
}