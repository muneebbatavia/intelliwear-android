package com.example.intelliwear;

import java.util.List;

// Helper class to represent a section
public class Section {
    private String title;
    private List<String> items;

    public Section(String title, List<String> items) {
        this.title = title;
        this.items = items;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getItems() {
        return items;
    }
}
