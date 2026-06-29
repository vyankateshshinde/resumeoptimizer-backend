package com.vyankatesh.resumeoptimizer.resumebuilder.dto;

import java.util.ArrayList;
import java.util.List;

public class ResumeSectionItem {

    private String id;
    private String type;
    private String key;
    private String title;
    private List<String> bullets = new ArrayList<>();
    private List<ResumeSubsectionItem> subSections = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getBullets() { return bullets; }
    public void setBullets(List<String> bullets) {
        this.bullets = bullets == null ? new ArrayList<>() : bullets;
    }

    public List<ResumeSubsectionItem> getSubSections() { return subSections; }
    public void setSubSections(List<ResumeSubsectionItem> subSections) {
        this.subSections = subSections == null ? new ArrayList<>() : subSections;
    }
}
