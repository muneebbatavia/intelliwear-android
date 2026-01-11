package com.example.intelliwear;

public class AIResponse {
    private String response;
    private long timestamp;

    public AIResponse() {
        // Required empty constructor
    }

    public AIResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
