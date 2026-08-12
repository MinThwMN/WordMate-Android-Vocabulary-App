package com.example.engapp.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.engapp.R;
import com.example.engapp.repositories.SettingsRepository;
import com.example.engapp.utils.LocaleHelper;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {
    // Thời gian hiển thị màn hình splash (900 ms)
    private static final long SPLASH_DELAY = 900;

    private SettingsRepository settingsRepository;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        settingsRepository = new SettingsRepository(this);
        firebaseAuth = FirebaseAuth.getInstance();

        // Chờ SPLASH_DELAY rồi gọi hàm điều hướng màn hình tiếp theo
        new Handler(Looper.getMainLooper()).postDelayed(
                this::navigateNext,
                SPLASH_DELAY
        );
    }

    // Kiểm tra trạng thái người dùng để quyết định màn hình tiếp theo
    private void navigateNext() {
         // Nếu user đã đăng nhập rồi thì vào thẳng MainActivity.
         // Không hiện lại Getting Started hoặc Language nữa.
        if (firebaseAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        // Nếu chưa đăng nhập và lần đầu mở app thì hiện màn giới thiệu trước.
        if (!settingsRepository.hasSeenGettingStarted()) {
            goToGettingStarted();
            return;
        }

        //Sau Getting Started mới tới chọn ngôn ngữ.
        if (!settingsRepository.hasSelectedLanguage()) {
            goToLanguage();
            return;
        }

        // Nếu đã xem giới thiệu + đã chọn ngôn ngữ nhưng chưa đăng nhập thì vào Login
        goToLogin();
    }

    private void goToGettingStarted() {
        Intent intent = new Intent(SplashActivity.this, GettingStartedActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToLanguage() {
        Intent intent = new Intent(SplashActivity.this, LanguageActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}