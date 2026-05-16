package com.example.bookrecommender.model;

import java.util.ArrayList;
import java.util.List;

public class BookInfo {
    private String id;
    private String title;
    private String author;
    private List<String> themes = new ArrayList<>();
    private String readingLevel;
    private String description;
    private List<String> recommendedUsers = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<String> getThemes() {
        return themes;
    }

    public void setThemes(List<String> themes) {
        this.themes = themes;
    }

    public String getReadingLevel() {
        return readingLevel;
    }

    public void setReadingLevel(String readingLevel) {
        this.readingLevel = readingLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRecommendedUsers() {
        return recommendedUsers;
    }

    public void setRecommendedUsers(List<String> recommendedUsers) {
        this.recommendedUsers = recommendedUsers;
    }
}
