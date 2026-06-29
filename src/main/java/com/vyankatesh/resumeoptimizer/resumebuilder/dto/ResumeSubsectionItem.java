package com.vyankatesh.resumeoptimizer.resumebuilder.dto;

import java.util.ArrayList;
import java.util.List;

public class ResumeSubsectionItem {

    private String id;
    private String title;
    private List<String> bullets = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getBullets() { return bullets; }
    public void setBullets(List<String> bullets) {
        this.bullets = bullets == null ? new ArrayList<>() : bullets;
    }
}
