package com.example.engapp.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.engapp.R;
import com.example.engapp.repositories.SettingsRepository;
import com.example.engapp.utils.LocaleHelper;

public class GettingStartedActivity extends AppCompatActivity {

    private TextView btnGetStarted;
    private SettingsRepository settingsRepository;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_getting_started);

        settingsRepository = new SettingsRepository(this);

        btnGetStarted = findViewById(R.id.btnGetStarted);

        btnGetStarted.setOnClickListener(v -> {
            settingsRepository.setHasSeenGettingStarted(true);

            Intent intent = new Intent(
                    GettingStartedActivity.this,
                    LanguageActivity.class
            );

            startActivity(intent);
            finish();
        });
    }
}