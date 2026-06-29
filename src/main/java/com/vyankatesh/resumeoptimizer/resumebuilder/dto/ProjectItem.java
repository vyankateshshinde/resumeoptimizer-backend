package com.vyankatesh.resumeoptimizer.resumebuilder.dto;

import java.util.ArrayList;
import java.util.List;

public class ProjectItem {

    private String name;
    private String startDate;
    private String endDate;
    private List<String> bullets = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<String> getBullets() {
        return bullets;
    }

    public void setBullets(List<String> bullets) {
        this.bullets = bullets;
    }
}
