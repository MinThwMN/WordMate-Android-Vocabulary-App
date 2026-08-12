package com.example.engapp.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class GoalRepository {

    private static final String PREF_NAME = "goal_pref";
    private static final String KEY_DAILY_GOAL = "daily_goal";

    private static final int DEFAULT_DAILY_GOAL = 5;

    private final SharedPreferences sharedPreferences;

    public GoalRepository(Context context) {
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

    public int getDailyGoal() {
        return sharedPreferences.getInt(
                getUserKey(KEY_DAILY_GOAL),
                DEFAULT_DAILY_GOAL
        );
    }

    public void saveDailyGoal(int goal) {
        if (goal <= 0) {
            goal = DEFAULT_DAILY_GOAL;
        }

        if (goal > 100) {
            goal = 100;
        }

        sharedPreferences.edit()
                .putInt(getUserKey(KEY_DAILY_GOAL), goal)
                .apply();
    }

    public void clearGoalData() {
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
}