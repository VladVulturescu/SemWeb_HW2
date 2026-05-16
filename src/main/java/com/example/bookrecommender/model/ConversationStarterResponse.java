package com.example.bookrecommender.model;

import java.util.ArrayList;
import java.util.List;

public class ConversationStarterResponse {
    private List<String> starters = new ArrayList<>();

    public ConversationStarterResponse() {
    }

    public ConversationStarterResponse(List<String> starters) {
        this.starters = starters;
    }

    public List<String> getStarters() {
        return starters;
    }

    public void setStarters(List<String> starters) {
        this.starters = starters;
    }
}