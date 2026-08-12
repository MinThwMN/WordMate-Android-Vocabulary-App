package com.example.engapp.models;

import java.util.List;

public class Topic {
    private String topicId;
    private String topicName;
    private String icon;
    private List<Word> words;

    public Topic() {
    }

    public Topic(String topicId, String topicName, String icon, List<Word> words) {
        this.topicId = topicId;
        this.topicName = topicName;
        this.icon = icon;
        this.words = words;
    }

    public String getTopicId() {
        return topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getIcon() {
        return icon;
    }

    public List<Word> getWords() {
        return words;
    }

    public int getTotalWords() {
        if (words == null) {
            return 0;
        }
        return words.size();
    }

    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }
}