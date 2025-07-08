package com.example.testproject.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.webkit.URLUtil;

import androidx.core.content.ContextCompat;

import com.example.testproject.database.DatabaseHelper;
import com.example.testproject.models.DownloadItem;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DownloadManagerHelper {
    private Context context;
    private DownloadManager downloadManager;
    private DatabaseHelper databaseHelper;
    private Map<Long, DownloadItem> downloadMap;
    private DownloadProgressListener progressListener;

    public interface DownloadProgressListener {
        void onDownloadProgress(DownloadItem item);
        void onDownloadCompleted(DownloadItem item);
        void onDownloadFailed(DownloadItem item);
    }

    public DownloadManagerHelper(Context context, DatabaseHelper databaseHelper) {
        this.context = context;
        this.databaseHelper = databaseHelper;
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.downloadMap = new HashMap<>();
        
        // Register broadcast receiver for download completion
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        ContextCompat.registerReceiver(context, downloadCompleteReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    public void setProgressListener(DownloadProgressListener listener) {
        this.progressListener = listener;
    }

    public long startDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        
        // Create download directory if it doesn't exist
        File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Browser");
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(fileName);
        request.setDescription("Downloading file...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS + "/Browser", fileName);
        request.addRequestHeader("User-Agent", userAgent);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);

        long downloadId = downloadManager.enqueue(request);

        // Create download item and save to database
        String filePath = downloadDir.getAbsolutePath() + "/" + fileName;
        DownloadItem downloadItem = new DownloadItem(fileName, url, fileName, filePath, 0, mimeType);
        downloadItem.setId(downloadId);
        
        databaseHelper.addDownloadItem(downloadItem);
        downloadMap.put(downloadId, downloadItem);

        return downloadId;
    }

    public void cancelDownload(long downloadId) {
        downloadManager.remove(downloadId);
        downloadMap.remove(downloadId);
        databaseHelper.deleteDownloadItem(downloadId);
    }

    public DownloadItem getDownloadProgress(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        
        Cursor cursor = downloadManager.query(query);
        if (cursor.moveToFirst()) {
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            int totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);

            int status = cursor.getInt(statusIndex);
            long downloaded = cursor.getLong(downloadedIndex);
            long total = cursor.getLong(totalIndex);

            DownloadItem item = downloadMap.get(downloadId);
            if (item != null) {
                item.setDownloadedSize(downloaded);
                item.setFileSize(total);
                
                switch (status) {
                    case DownloadManager.STATUS_RUNNING:
                        item.setStatus(DownloadItem.STATUS_DOWNLOADING);
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        item.setStatus(DownloadItem.STATUS_COMPLETED);
                        break;
                    case DownloadManager.STATUS_FAILED:
                        item.setStatus(DownloadItem.STATUS_FAILED);
                        break;
                    case DownloadManager.STATUS_PAUSED:
                        item.setStatus(DownloadItem.STATUS_PAUSED);
                        break;
                }
                
                databaseHelper.updateDownload(item);
            }
        }
        cursor.close();
        
        return downloadMap.get(downloadId);
    }

    private BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId != -1 && downloadMap.containsKey(downloadId)) {
                DownloadItem item = getDownloadProgress(downloadId);
                if (item != null) {
                    if (progressListener != null) {
                        if (item.getStatus() == DownloadItem.STATUS_COMPLETED) {
                            progressListener.onDownloadCompleted(item);
                        } else if (item.getStatus() == DownloadItem.STATUS_FAILED) {
                            progressListener.onDownloadFailed(item);
                        }
                    }
                }
            }
        }
    };

    public void cleanup() {
        try {
            context.unregisterReceiver(downloadCompleteReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }
}
