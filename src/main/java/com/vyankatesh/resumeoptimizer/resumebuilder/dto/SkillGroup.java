package com.vyankatesh.resumeoptimizer.resumebuilder.dto;

import java.util.ArrayList;
import java.util.List;

public class SkillGroup {

    private String category;
    private List<String> items = new ArrayList<>();

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }
}
