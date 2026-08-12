package com.example.engapp.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.engapp.R;
import com.example.engapp.repositories.SettingsRepository;
import com.example.engapp.utils.LocaleHelper;

public class LanguageActivity extends AppCompatActivity {

    // Layout lựa chọn ngôn ngữ
    private View layoutVietnamese;
    private View layoutEnglish;
    private View layoutJapanese;
    private View layoutKorean;

    // Repository dùng để lưu ngôn ngữ người dùng đã chọn
    private SettingsRepository settingsRepository;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        settingsRepository = new SettingsRepository(this);

        initViews();
        setupEvents();
    }

    private void initViews() {
        layoutVietnamese = findViewById(R.id.layoutVietnamese);
        layoutEnglish = findViewById(R.id.layoutEnglish);
        layoutJapanese = findViewById(R.id.layoutJapanese);
        layoutKorean = findViewById(R.id.layoutKorean);
    }

    private void setupEvents() {
        layoutVietnamese.setOnClickListener(v -> selectLanguage(SettingsRepository.LANGUAGE_VI));
        layoutEnglish.setOnClickListener(v -> selectLanguage(SettingsRepository.LANGUAGE_EN));
        layoutJapanese.setOnClickListener(v -> selectLanguage(SettingsRepository.LANGUAGE_JA));
        layoutKorean.setOnClickListener(v -> selectLanguage(SettingsRepository.LANGUAGE_KO));
    }

    private void selectLanguage(String languageCode) {
        settingsRepository.setLanguageCode(languageCode);
        settingsRepository.setHasSelectedLanguage(true);

        Intent intent = new Intent(LanguageActivity.this, LoginActivity.class);

        // Xóa các màn hình trước đó để người dùng không quay lại màn hình chọn ngôn ngữ bằng nút Back
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
}