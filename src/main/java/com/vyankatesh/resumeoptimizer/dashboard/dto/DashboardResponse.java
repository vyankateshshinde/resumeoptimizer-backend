package com.vyankatesh.resumeoptimizer.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private Long totalResumes;

    private Long totalAnalyses;

    private Integer highestScore;

    private Double averageScore;
}