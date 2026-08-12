package com.example.engapp.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.engapp.models.WordList;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FavoriteRepository {

    private static final String PREF_NAME = "favorite_lists_pref";

    private static final String KEY_LIST_IDS = "favorite_list_ids";
    private static final String KEY_LIST_NAME_PREFIX = "favorite_list_name_";
    private static final String KEY_LIST_WORDS_PREFIX = "favorite_list_words_";

    private final SharedPreferences sharedPreferences;

    public FavoriteRepository(Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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

    private String getListNameKey(String listId) {
        return getUserKey(KEY_LIST_NAME_PREFIX + listId);
    }

    private String getListWordsKey(String listId) {
        return getUserKey(KEY_LIST_WORDS_PREFIX + listId);
    }

    public WordList createWordList(String listName) {
        if (listName == null || listName.trim().isEmpty()) {
            listName = "New List";
        }

        String listId = "list_" + UUID.randomUUID().toString();

        Set<String> listIds = getStringSet(getUserKey(KEY_LIST_IDS));
        listIds.add(listId);

        sharedPreferences.edit()
                .putStringSet(getUserKey(KEY_LIST_IDS), listIds)
                .putString(getListNameKey(listId), listName.trim())
                .putStringSet(getListWordsKey(listId), new HashSet<>())
                .apply();

        return new WordList(listId, listName.trim(), 0);
    }

    public List<WordList> getAllWordLists() {
        List<WordList> wordLists = new ArrayList<>();

        Set<String> listIds = getStringSet(getUserKey(KEY_LIST_IDS));

        for (String listId : listIds) {
            if (listId == null || listId.trim().isEmpty()) {
                continue;
            }

            String listName = sharedPreferences.getString(
                    getListNameKey(listId),
                    "Unnamed List"
            );

            int totalWords = getWordIdsByListId(listId).size();

            wordLists.add(new WordList(listId, listName, totalWords));
        }

        return wordLists;
    }

    public WordList getWordListById(String listId) {
        if (listId == null || listId.trim().isEmpty()) {
            return null;
        }

        String listName = sharedPreferences.getString(
                getListNameKey(listId),
                null
        );

        if (listName == null) {
            return null;
        }

        int totalWords = getWordIdsByListId(listId).size();

        return new WordList(listId, listName, totalWords);
    }

    public void addWordToList(String listId, String wordId) {
        if (listId == null || listId.trim().isEmpty()
                || wordId == null || wordId.trim().isEmpty()) {
            return;
        }

        Set<String> wordIds = getStringSet(getListWordsKey(listId));
        wordIds.add(wordId);

        sharedPreferences.edit()
                .putStringSet(getListWordsKey(listId), wordIds)
                .apply();
    }

    public void removeWordFromList(String listId, String wordId) {
        if (listId == null || listId.trim().isEmpty()
                || wordId == null || wordId.trim().isEmpty()) {
            return;
        }

        Set<String> wordIds = getStringSet(getListWordsKey(listId));
        wordIds.remove(wordId);

        sharedPreferences.edit()
                .putStringSet(getListWordsKey(listId), wordIds)
                .apply();
    }

    public boolean isWordInList(String listId, String wordId) {
        if (listId == null || listId.trim().isEmpty()
                || wordId == null || wordId.trim().isEmpty()) {
            return false;
        }

        Set<String> wordIds = getStringSet(getListWordsKey(listId));

        return wordIds.contains(wordId);
    }

    public boolean isWordSavedInAnyList(String wordId) {
        if (wordId == null || wordId.trim().isEmpty()) {
            return false;
        }

        List<WordList> lists = getAllWordLists();

        for (WordList list : lists) {
            if (list != null && isWordInList(list.getListId(), wordId)) {
                return true;
            }
        }

        return false;
    }

    public List<String> getWordIdsByListId(String listId) {
        if (listId == null || listId.trim().isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> wordIds = getStringSet(getListWordsKey(listId));

        return new ArrayList<>(wordIds);
    }

    public void deleteWordList(String listId) {
        if (listId == null || listId.trim().isEmpty()) {
            return;
        }

        Set<String> listIds = getStringSet(getUserKey(KEY_LIST_IDS));
        listIds.remove(listId);

        sharedPreferences.edit()
                .putStringSet(getUserKey(KEY_LIST_IDS), listIds)
                .remove(getListNameKey(listId))
                .remove(getListWordsKey(listId))
                .apply();
    }

    public int getTotalSavedWords() {
        Set<String> allSavedWordIds = new HashSet<>();

        List<WordList> lists = getAllWordLists();

        for (WordList list : lists) {
            if (list != null && list.getListId() != null) {
                allSavedWordIds.addAll(getWordIdsByListId(list.getListId()));
            }
        }

        return allSavedWordIds.size();
    }

    public void clearFavoriteData() {
        String prefix = getUserPrefix();

        SharedPreferences.Editor editor = sharedPreferences.edit();

        for (String key : sharedPreferences.getAll().keySet()) {
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