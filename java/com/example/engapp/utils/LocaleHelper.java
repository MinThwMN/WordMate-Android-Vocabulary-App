package com.example.engapp.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import com.example.engapp.repositories.SettingsRepository;

import java.util.Locale;

public class LocaleHelper {

    public static Context applyLanguage(Context context) {
        SettingsRepository settingsRepository = new SettingsRepository(context);
        String languageCode = settingsRepository.getLanguageCode();

        return setLocale(context, languageCode);
    }

    public static String getSavedLanguage(Context context) {
        SettingsRepository settingsRepository = new SettingsRepository(context);
        String languageCode = settingsRepository.getLanguageCode();

        if (languageCode == null || languageCode.trim().isEmpty()) {
            return SettingsRepository.LANGUAGE_VI;
        }

        return languageCode;
    }

    public static Context setLocale(Context context, String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            languageCode = SettingsRepository.LANGUAGE_VI;
        }

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }
}