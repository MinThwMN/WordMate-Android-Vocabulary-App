package com.example.engapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.engapp.repositories.SettingsRepository;
import com.example.engapp.utils.ReminderScheduler;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SettingsRepository settingsRepository = new SettingsRepository(context);

            if (settingsRepository.isReminderEnabled()) {
                ReminderScheduler.scheduleDailyReminder(context);
            }
        }
    }
}