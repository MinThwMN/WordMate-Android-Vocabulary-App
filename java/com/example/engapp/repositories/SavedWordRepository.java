package com.example.engapp.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.engapp.models.Word;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SavedWordRepository {

    private static final String PREF_NAME = "saved_api_words_pref";
    private static final String KEY_API_WORD_IDS = "api_word_ids";
    private static final String KEY_API_WORD_PREFIX = "api_word_";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public SavedWordRepository(Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        gson = new Gson();
    }

    private String getUserPrefix() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getUid() != null && !user.getUid().trim().isEmpty()) {
            return "user_" + user.getUid() + "_";
        }

        return "guest_";
    }

    private String getUserKey(String key) {
        return getUserPrefix() + key;
    }

    private String getApiWordKey(String wordId) {
        return getUserKey(KEY_API_WORD_PREFIX + wordId);
    }

    public void saveWord(Word word) {
        if (word == null || word.getWordId() == null || word.getWordId().trim().isEmpty()) {
            return;
        }

        Set<String> wordIds = getStringSet(getUserKey(KEY_API_WORD_IDS));
        wordIds.add(word.getWordId());

        String json = gson.toJson(word);

        sharedPreferences.edit()
                .putStringSet(getUserKey(KEY_API_WORD_IDS), wordIds)
                .putString(getApiWordKey(word.getWordId()), json)
                .apply();
    }

    public Word getWordById(String wordId) {
        if (wordId == null || wordId.trim().isEmpty()) {
            return null;
        }

        String json = sharedPreferences.getString(getApiWordKey(wordId), null);

        if (json == null) {
            return null;
        }

        try {
            return gson.fromJson(json, Word.class);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Word> getAllSavedApiWords() {
        List<Word> words = new ArrayList<>();

        Set<String> wordIds = getStringSet(getUserKey(KEY_API_WORD_IDS));

        for (String wordId : wordIds) {
            Word word = getWordById(wordId);

            if (word != null) {
                words.add(word);
            }
        }

        return words;
    }

    public void deleteWord(String wordId) {
        if (wordId == null || wordId.trim().isEmpty()) {
            return;
        }

        Set<String> wordIds = getStringSet(getUserKey(KEY_API_WORD_IDS));
        wordIds.remove(wordId);

        sharedPreferences.edit()
                .putStringSet(getUserKey(KEY_API_WORD_IDS), wordIds)
                .remove(getApiWordKey(wordId))
                .apply();
    }

    public boolean isWordSaved(String wordId) {
        if (wordId == null || wordId.trim().isEmpty()) {
            return false;
        }

        return getStringSet(getUserKey(KEY_API_WORD_IDS)).contains(wordId);
    }

    public void clearSavedWords() {
        String prefix = getUserPrefix();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        Map<String, ?> allData = sharedPreferences.getAll();

        for (String key : allData.keySet()) {
            if (key != null && key.startsWith(prefix)) {
                editor.remove(key);
            }
        }

        editor.apply();
    }

    private Set<String> getStringSet(String key) {
        Set<String> savedSet = sharedPreferences.getStringSet(key, new HashSet<>());

        if (savedSet == null) {
            return new HashSet<>();
        }

        return new HashSet<>(savedSet);
    }
}