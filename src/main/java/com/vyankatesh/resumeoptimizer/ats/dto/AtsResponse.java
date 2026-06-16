package com.vyankatesh.resumeoptimizer.ats.dto;

import java.util.List;

public class AtsResponse {

    private int skillScore;
    private int keywordScore;
    private int finalScore;

    private List<String> matchedSkills;
    private List<String> missingSkills;

    private String feedback;

    // getters and setters

    public int getSkillScore() {
        return skillScore;
    }

    public void setSkillScore(int skillScore) {
        this.skillScore = skillScore;
    }

    public int getKeywordScore() {
        return keywordScore;
    }

    public void setKeywordScore(int keywordScore) {
        this.keywordScore = keywordScore;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(int finalScore) {
        this.finalScore = finalScore;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}