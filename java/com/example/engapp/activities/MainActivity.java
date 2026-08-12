package com.example.engapp.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.engapp.R;
import com.example.engapp.fragments.DashboardFragment;
import com.example.engapp.fragments.FavoriteFragment;
import com.example.engapp.fragments.HomeFragment;
import com.example.engapp.fragments.SearchFragment;
import com.example.engapp.fragments.SettingsFragment;
import com.example.engapp.repositories.FirebaseUserDataRepository;
import com.example.engapp.repositories.SettingsRepository;
import com.example.engapp.utils.LocaleHelper;
import com.example.engapp.utils.ReminderScheduler;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    // Thanh điều hướng dưới cùng để chuyển giữa các màn hình chính
    private BottomNavigationView bottomNavigationView;
    private SettingsRepository settingsRepository;

    // Repository dùng để khôi phục và đồng bộ dữ liệu người dùng với Firebase
    private FirebaseUserDataRepository firebaseUserDataRepository;

    private boolean isFirstFragmentLoaded = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settingsRepository = new SettingsRepository(this);
        applySavedTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        firebaseUserDataRepository = new FirebaseUserDataRepository(this);

        setupBottomNavigation();
        scheduleReminderIfAllowed();

        if (savedInstanceState == null) {
            restoreFirebaseThenLoadHome();
        } else {
            isFirstFragmentLoaded = true;
        }
    }

    // Kiểm tra cài đặt và quyền thông báo trước khi đặt lịch nhắc học hằng ngày
    private void scheduleReminderIfAllowed() {
        if (!settingsRepository.isReminderEnabled()) {
            ReminderScheduler.cancelReminder(this);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        ReminderScheduler.scheduleDailyReminder(this);
    }

    // Khôi phục dữ liệu từ Firebase trước, sau đó mới load màn hình Home
    private void restoreFirebaseThenLoadHome() {
        firebaseUserDataRepository.restoreCloudThenStartAutoSync(
                new FirebaseUserDataRepository.SyncCallback() {
                    @Override
                    public void onSuccess(boolean hasCloudData) {
                        Log.d("FirebaseSync", "Restore done. Has cloud data: " + hasCloudData);
                        loadFirstFragmentIfNeeded();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("FirebaseSync", "Restore failed", e);

                        Toast.makeText(
                                MainActivity.this,
                                getString(R.string.sync_data_failed),
                                Toast.LENGTH_SHORT
                        ).show();

                        loadFirstFragmentIfNeeded();
                    }
                }
        );
    }

    private void loadFirstFragmentIfNeeded() {
        if (isFirstFragmentLoaded) {
            return;
        }

        isFirstFragmentLoaded = true;

        loadFragment(new HomeFragment());
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_search) {
                loadFragment(new SearchFragment());
                return true;
            } else if (itemId == R.id.nav_dashboard) {
                loadFragment(new DashboardFragment());
                return true;
            } else if (itemId == R.id.nav_favorite) {
                loadFragment(new FavoriteFragment());
                return true;
            } else if (itemId == R.id.nav_settings) {
                loadFragment(new SettingsFragment());
                return true;
            }

            return false;
        });
    }

    private void applySavedTheme() {
        if (settingsRepository.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    // Thay thế Fragment hiện tại bằng Fragment mới trong fragmentContainer
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // Dừng tự động đồng bộ Firebase khi MainActivity bị hủy
    @Override
    protected void onDestroy() {
        if (firebaseUserDataRepository != null) {
            firebaseUserDataRepository.stopAutoSync();
        }

        super.onDestroy();
    }
}
