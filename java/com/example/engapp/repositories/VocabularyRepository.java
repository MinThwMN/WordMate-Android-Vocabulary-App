package com.example.engapp.repositories;

import android.content.Context;

import com.example.engapp.models.Topic;
import com.example.engapp.models.Word;
import com.example.engapp.utils.JsonHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class VocabularyRepository {

    private List<Topic> topics;

    public VocabularyRepository(Context context) {
        loadVocabulary(context);
    }

    private void loadVocabulary(Context context) {
        String json = JsonHelper.readJsonFromAssets(context, "vocabulary.json");

        if (json == null || json.isEmpty()) {
            topics = new ArrayList<>();
            return;
        }

        Type type = new TypeToken<List<Topic>>() {}.getType();
        topics = new Gson().fromJson(json, type);

        if (topics == null) {
            topics = new ArrayList<>();
        }
    }

    public List<Topic> getAllTopics() {
        return topics;
    }

    public List<Word> getWordsByTopicId(String topicId) {
        for (Topic topic : topics) {
            if (topic.getTopicId().equals(topicId)) {
                return topic.getWords();
            }
        }

        return new ArrayList<>();
    }

    public List<Word> getAllWords() {
        List<Word> allWords = new ArrayList<>();
        List<Topic> topics = getAllTopics();

        if (topics == null) {
            return allWords;
        }

        for (Topic topic : topics) {
            if (topic.getWords() != null) {
                allWords.addAll(topic.getWords());
            }
        }

        return allWords;
    }

    public Word getWordById(String wordId) {
        if (wordId == null) {
            return null;
        }

        List<Word> allWords = getAllWords();

        for (Word word : allWords) {
            if (word.getWordId() != null && word.getWordId().equals(wordId)) {
                return word;
            }
        }

        return null;
    }

    public Topic getTopicById(String topicId) {
        for (Topic topic : topics) {
            if (topic.getTopicId().equals(topicId)) {
                return topic;
            }
        }

        return null;
    }
}