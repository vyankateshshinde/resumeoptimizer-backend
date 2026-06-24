package com.vyankatesh.resumeoptimizer.resumeversion.dto;

import java.util.List;

public class ResumeComparisonResponse {

    private String version1Name;
    private String version2Name;

    private int atsScoreDifference;

    private List<String> addedSkills;
    private List<String> removedSkills;

    private String comparisonSummary;

    public String getVersion1Name() {
        return version1Name;
    }

    public void setVersion1Name(String version1Name) {
        this.version1Name = version1Name;
    }

    public String getVersion2Name() {
        return version2Name;
    }

    public void setVersion2Name(String version2Name) {
        this.version2Name = version2Name;
    }

    public int getAtsScoreDifference() {
        return atsScoreDifference;
    }

    public void setAtsScoreDifference(int atsScoreDifference) {
        this.atsScoreDifference = atsScoreDifference;
    }

    public List<String> getAddedSkills() {
        return addedSkills;
    }

    public void setAddedSkills(List<String> addedSkills) {
        this.addedSkills = addedSkills;
    }

    public List<String> getRemovedSkills() {
        return removedSkills;
    }

    public void setRemovedSkills(List<String> removedSkills) {
        this.removedSkills = removedSkills;
    }

    public String getComparisonSummary() {
        return comparisonSummary;
    }

    public void setComparisonSummary(String comparisonSummary) {
        this.comparisonSummary = comparisonSummary;
    }
}