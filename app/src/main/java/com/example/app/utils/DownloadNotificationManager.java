package com.example.app.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.app.DownloadsActivity;
import com.example.app.R;
import com.example.app.models.DownloadItem;

import java.util.Locale;

public class DownloadNotificationManager {
    private static final String CHANNEL_ID = "downloads";
    private static final String CHANNEL_NAME = "Downloads";
    private Context context;
    private NotificationManager notificationManager;

    public DownloadNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Download progress notifications");
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showDownloadProgress(DownloadItem item) {
        Intent intent = new Intent(context, DownloadsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Downloading " + item.getTitle())
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)  // Only alert once to avoid spam
            .setPriority(NotificationCompat.PRIORITY_LOW);

        if (item.getFileSize() > 0) {
            int progress = item.getProgress();
            builder.setProgress(100, progress, false)
                   .setContentText(progress + "% - " + formatFileSize(item.getDownloadedSize()) + 
                                 " of " + formatFileSize(item.getFileSize()));
        } else {
            builder.setProgress(0, 0, true)
                   .setContentText("Downloading...");
        }

        notificationManager.notify((int) item.getId(), builder.build());
    }

    public void showDownloadCompleted(DownloadItem item) {
        // First cancel the ongoing progress notification
        cancelNotification(item.getId());
        
        Intent intent = new Intent(context, DownloadsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download completed")
            .setContentText(item.getTitle())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify((int) item.getId(), builder.build());
    }

    public void showDownloadFailed(DownloadItem item, String error) {
        // First cancel the ongoing progress notification
        cancelNotification(item.getId());
        
        Intent intent = new Intent(context, DownloadsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download failed")
            .setContentText(item.getTitle())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify((int) item.getId(), builder.build());
    }

    public void cancelNotification(long downloadId) {
        notificationManager.cancel((int) downloadId);
    }

    public void clearAllNotifications() {
        notificationManager.cancelAll();
    }
    
    public void clearCompletedNotifications() {
        // This method can be called periodically to clean up completed notifications
        // For now, we'll just cancel all notifications that might be stuck
        notificationManager.cancelAll();
    }

    private String formatFileSize(long bytes) {
        if (bytes == 0) return "0 B";
        
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format(Locale.ROOT, "%.1f %s", size, units[unitIndex]);
    }
}
