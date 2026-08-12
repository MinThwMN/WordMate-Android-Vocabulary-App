package com.example.engapp.network.models;

import java.util.List;

public class DatamuseSuggestion {

    private String word;
    private int score;
    private List<String> tags;

    public DatamuseSuggestion() {
        // Required empty constructor
    }

    public DatamuseSuggestion(String word, int score, List<String> tags) {
        this.word = word;
        this.score = score;
        this.tags = tags;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}