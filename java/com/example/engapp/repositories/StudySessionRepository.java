package com.example.engapp.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class StudySessionRepository {

    private static final String PREF_NAME = "study_session_pref";

    private static final String KEY_LAST_TOPIC_ID = "last_topic_id";
    private static final String KEY_LAST_TOPIC_NAME = "last_topic_name";

    private final SharedPreferences sharedPreferences;

    public StudySessionRepository(Context context) {
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

    public void saveLastTopic(String topicId, String topicName) {
        if (topicId == null || topicId.trim().isEmpty()) {
            return;
        }

        if (topicName == null || topicName.trim().isEmpty()) {
            return;
        }

        sharedPreferences.edit()
                .putString(getUserKey(KEY_LAST_TOPIC_ID), topicId)
                .putString(getUserKey(KEY_LAST_TOPIC_NAME), topicName)
                .apply();
    }

    public String getLastTopicId() {
        return sharedPreferences.getString(getUserKey(KEY_LAST_TOPIC_ID), null);
    }

    public String getLastTopicName() {
        return sharedPreferences.getString(getUserKey(KEY_LAST_TOPIC_NAME), null);
    }

    public void clearLastTopic() {
        sharedPreferences.edit()
                .remove(getUserKey(KEY_LAST_TOPIC_ID))
                .remove(getUserKey(KEY_LAST_TOPIC_NAME))
                .apply();
    }
}