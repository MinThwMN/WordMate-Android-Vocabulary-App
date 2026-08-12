package com.example.engapp.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.engapp.R;

public class SettingsRepository {

    private static final String PREF_NAME = "settings_pref";

    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_LANGUAGE = "language_code";
    private static final String KEY_REMINDER_TIME = "reminder_time";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";

    private static final String KEY_HAS_SEEN_GETTING_STARTED = "has_seen_getting_started";
    private static final String KEY_HAS_SELECTED_LANGUAGE = "has_selected_language";

    public static final String LANGUAGE_VI = "vi";
    public static final String LANGUAGE_EN = "en";
    public static final String LANGUAGE_JA = "ja";
    public static final String LANGUAGE_KO = "ko";

    private final Context context;
    private final SharedPreferences sharedPreferences;

    public SettingsRepository(Context context) {
        this.context = context.getApplicationContext();
        sharedPreferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isDarkMode() {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean enabled) {
        sharedPreferences.edit()
                .putBoolean(KEY_DARK_MODE, enabled)
                .apply();
    }

    public boolean isSoundEnabled() {
        return sharedPreferences.getBoolean(KEY_SOUND_ENABLED, true);
    }

    public void setSoundEnabled(boolean enabled) {
        sharedPreferences.edit()
                .putBoolean(KEY_SOUND_ENABLED, enabled)
                .apply();
    }

    public String getLanguageCode() {
        return sharedPreferences.getString(KEY_LANGUAGE, LANGUAGE_VI);
    }

    public void setLanguageCode(String languageCode) {
        if (!isSupportedLanguage(languageCode)) {
            languageCode = LANGUAGE_VI;
        }

        sharedPreferences.edit()
                .putString(KEY_LANGUAGE, languageCode)
                .apply();
    }

    public String getLanguageName() {
        String code = getLanguageCode();

        switch (code) {
            case LANGUAGE_EN:
                return context.getString(R.string.language_english);

            case LANGUAGE_JA:
                return context.getString(R.string.language_japanese);

            case LANGUAGE_KO:
                return context.getString(R.string.language_korean);

            case LANGUAGE_VI:
            default:
                return context.getString(R.string.language_vietnamese);
        }
    }

    private boolean isSupportedLanguage(String languageCode) {
        return LANGUAGE_VI.equals(languageCode)
                || LANGUAGE_EN.equals(languageCode)
                || LANGUAGE_JA.equals(languageCode)
                || LANGUAGE_KO.equals(languageCode);
    }

    public boolean hasSeenGettingStarted() {
        return sharedPreferences.getBoolean(KEY_HAS_SEEN_GETTING_STARTED, false);
    }

    public void setHasSeenGettingStarted(boolean hasSeen) {
        sharedPreferences.edit()
                .putBoolean(KEY_HAS_SEEN_GETTING_STARTED, hasSeen)
                .apply();
    }

    public boolean hasSelectedLanguage() {
        return sharedPreferences.getBoolean(KEY_HAS_SELECTED_LANGUAGE, false);
    }

    public void setHasSelectedLanguage(boolean hasSelected) {
        sharedPreferences.edit()
                .putBoolean(KEY_HAS_SELECTED_LANGUAGE, hasSelected)
                .apply();
    }

    public String getReminderTime() {
        return sharedPreferences.getString(KEY_REMINDER_TIME, "20:00");
    }

    public void setReminderTime(String time) {
        sharedPreferences.edit()
                .putString(KEY_REMINDER_TIME, time)
                .apply();
    }

    public boolean isReminderEnabled() {
        return sharedPreferences.getBoolean(KEY_REMINDER_ENABLED, false);
    }

    public void setReminderEnabled(boolean enabled) {
        sharedPreferences.edit()
                .putBoolean(KEY_REMINDER_ENABLED, enabled)
                .apply();
    }
}