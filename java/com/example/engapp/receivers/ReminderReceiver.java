package com.example.engapp.receivers;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.engapp.R;
import com.example.engapp.activities.MainActivity;
import com.example.engapp.repositories.SettingsRepository;
import com.example.engapp.utils.LocaleHelper;
import com.example.engapp.utils.ReminderScheduler;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "wordmate_reminder_channel";
    private static final int NOTIFICATION_ID = 3001;

    @Override
    public void onReceive(Context context, Intent intent) {
        SettingsRepository settingsRepository = new SettingsRepository(context);

        if (!settingsRepository.isReminderEnabled()) {
            return;
        }

        Context localizedContext = LocaleHelper.applyLanguage(context);

        createNotificationChannel(localizedContext);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    localizedContext,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent openAppIntent = new Intent(localizedContext, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                localizedContext,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(localizedContext, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_logo)
                        .setContentTitle(localizedContext.getString(R.string.app_name))
                        .setContentText(localizedContext.getString(R.string.reminder_notification_text))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(localizedContext.getString(R.string.reminder_notification_big_text)))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(localizedContext);

        notificationManager.notify(NOTIFICATION_ID, builder.build());

        ReminderScheduler.scheduleDailyReminder(context);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );

        channel.setDescription(context.getString(R.string.reminder_channel_description));

        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
}