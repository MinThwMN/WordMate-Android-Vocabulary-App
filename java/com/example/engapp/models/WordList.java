package com.example.engapp.models;

public class WordList {

    private String listId;
    private String listName;
    private int totalWords;

    public WordList() {
    }

    public WordList(String listId, String listName, int totalWords) {
        this.listId = listId;
        this.listName = listName;
        this.totalWords = totalWords;
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public int getTotalWords() {
        return totalWords;
    }

    public void setTotalWords(int totalWords) {
        this.totalWords = totalWords;
    }
}