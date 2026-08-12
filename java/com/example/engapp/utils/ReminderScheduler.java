package com.example.engapp.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.engapp.receivers.ReminderReceiver;
import com.example.engapp.repositories.SettingsRepository;

import java.util.Calendar;

public class ReminderScheduler {

    private static final int REMINDER_REQUEST_CODE = 2001;

    public static void scheduleDailyReminder(Context context) {
        Context appContext = context.getApplicationContext();

        SettingsRepository settingsRepository = new SettingsRepository(appContext);

        if (!settingsRepository.isReminderEnabled()) {
            cancelReminder(appContext);
            return;
        }

        String reminderTime = settingsRepository.getReminderTime();

        int hour = 20;
        int minute = 0;

        try {
            String[] parts = reminderTime.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception ignored) {
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        scheduleExactReminder(appContext, calendar.getTimeInMillis());
    }

    private static void scheduleExactReminder(Context context, long triggerTimeMillis) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setPackage(context.getPackageName());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            return;
        }

        alarmManager.cancel(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                );
            } else {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                );
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
            );
        }
    }

    public static void cancelReminder(Context context) {
        Context appContext = context.getApplicationContext();

        Intent intent = new Intent(appContext, ReminderReceiver.class);
        intent.setPackage(appContext.getPackageName());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                appContext,
                REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}