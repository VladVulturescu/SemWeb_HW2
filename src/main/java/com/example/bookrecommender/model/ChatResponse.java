package com.example.bookrecommender.model;

import java.util.ArrayList;
import java.util.List;

public class ChatResponse {
    private String answer;
    private List<String> retrievedBooks = new ArrayList<>();

    public ChatResponse() {
    }

    public ChatResponse(String answer, List<String> retrievedBooks) {
        this.answer = answer;
        this.retrievedBooks = retrievedBooks;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getRetrievedBooks() {
        return retrievedBooks;
    }

    public void setRetrievedBooks(List<String> retrievedBooks) {
        this.retrievedBooks = retrievedBooks;
    }
}