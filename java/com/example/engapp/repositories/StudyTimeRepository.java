package com.example.engapp.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StudyTimeRepository {

    private static final String PREF_NAME = "study_time_pref";

    private static final String KEY_TOTAL_STUDY_SECONDS = "total_study_seconds";
    private static final String KEY_STUDY_DATES = "study_time_dates";
    private static final String KEY_DAY_STUDY_SECONDS_PREFIX = "day_study_seconds_";

    private final SharedPreferences sharedPreferences;
    private final SimpleDateFormat dateFormat;

    public StudyTimeRepository(Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
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

    private String getDayStudySecondsKey(String date) {
        return getUserKey(KEY_DAY_STUDY_SECONDS_PREFIX + date);
    }

    public void addStudyTime(long seconds) {
        if (seconds <= 0) {
            return;
        }

        String today = getTodayDate();

        long totalSeconds = sharedPreferences.getLong(
                getUserKey(KEY_TOTAL_STUDY_SECONDS),
                0
        );

        long todaySeconds = sharedPreferences.getLong(
                getDayStudySecondsKey(today),
                0
        );

        Set<String> studyDates = getStringSet(getUserKey(KEY_STUDY_DATES));
        studyDates.add(today);

        sharedPreferences.edit()
                .putLong(getUserKey(KEY_TOTAL_STUDY_SECONDS), totalSeconds + seconds)
                .putLong(getDayStudySecondsKey(today), todaySeconds + seconds)
                .putStringSet(getUserKey(KEY_STUDY_DATES), studyDates)
                .apply();
    }

    public int getTotalStudyMinutes() {
        long seconds = sharedPreferences.getLong(
                getUserKey(KEY_TOTAL_STUDY_SECONDS),
                0
        );

        return (int) Math.ceil(seconds / 60.0);
    }

    public int getTodayStudyMinutes() {
        String today = getTodayDate();

        long seconds = sharedPreferences.getLong(
                getDayStudySecondsKey(today),
                0
        );

        return (int) Math.ceil(seconds / 60.0);
    }

    public long getStudySecondsByDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return 0;
        }

        return sharedPreferences.getLong(
                getDayStudySecondsKey(date),
                0
        );
    }

    public int getCurrentStudyTimeStreak() {
        Set<String> studyDates = getStringSet(getUserKey(KEY_STUDY_DATES));

        if (studyDates.isEmpty()) {
            return 0;
        }

        int streak = 0;
        Calendar calendar = Calendar.getInstance();

        while (true) {
            String date = dateFormat.format(calendar.getTime());

            if (studyDates.contains(date)) {
                streak++;
                calendar.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }

        return streak;
    }

    public void clearStudyTime() {
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

    private String getTodayDate() {
        return dateFormat.format(Calendar.getInstance().getTime());
    }
}