package com.example.testproject.utils;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.webkit.URLUtil;

import com.example.testproject.database.DatabaseHelper;
import com.example.testproject.models.DownloadItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChromeStyleDownloader {
    private Context context;
    private DatabaseHelper databaseHelper;
    private Map<Long, DownloadTask> activeDownloads;
    private DownloadProgressListener progressListener;
    private long nextDownloadId = 1;
    private ExecutorService executorService;
    private Handler mainHandler;

    public interface DownloadProgressListener {
        void onDownloadProgress(DownloadItem item);
        void onDownloadCompleted(DownloadItem item);
        void onDownloadFailed(DownloadItem item, String error);
        void onDownloadStarted(DownloadItem item);
    }

    public ChromeStyleDownloader(Context context, DatabaseHelper databaseHelper) {
        this.context = context;
        this.databaseHelper = databaseHelper;
        this.activeDownloads = new HashMap<>();
        this.executorService = Executors.newFixedThreadPool(3); // Allow 3 concurrent downloads
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setProgressListener(DownloadProgressListener listener) {
        this.progressListener = listener;
    }

    public long startDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        
        // Create downloads directory
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        // Handle duplicate filenames
        File file = new File(downloadDir, fileName);
        String originalName = fileName;
        String nameWithoutExt = "";
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        } else {
            nameWithoutExt = fileName;
        }

        int counter = 1;
        while (file.exists()) {
            fileName = nameWithoutExt + " (" + counter + ")" + extension;
            file = new File(downloadDir, fileName);
            counter++;
        }

        long downloadId = nextDownloadId++;
        String filePath = file.getAbsolutePath();
        
        DownloadItem downloadItem = new DownloadItem(fileName, url, fileName, filePath, 0, mimeType);
        downloadItem.setId(downloadId);
        downloadItem.setStatus(DownloadItem.STATUS_DOWNLOADING);
        
        // Save to database
        databaseHelper.addDownloadItem(downloadItem);
        
        // Start download task
        DownloadTask task = new DownloadTask(downloadItem, userAgent);
        activeDownloads.put(downloadId, task);
        executorService.execute(() -> task.doDownload(url));
        
        if (progressListener != null) {
            progressListener.onDownloadStarted(downloadItem);
        }
        
        return downloadId;
    }

    public void cancelDownload(long downloadId) {
        DownloadTask task = activeDownloads.get(downloadId);
        if (task != null) {
            task.cancel();
            activeDownloads.remove(downloadId);
        }
        databaseHelper.deleteDownloadItem(downloadId);
    }

    public boolean isDownloading(long downloadId) {
        return activeDownloads.containsKey(downloadId);
    }

    private class DownloadTask {
        private DownloadItem downloadItem;
        private String userAgent;
        private String errorMessage;
        private volatile boolean cancelled = false;

        public DownloadTask(DownloadItem downloadItem, String userAgent) {
            this.downloadItem = downloadItem;
            this.userAgent = userAgent;
        }

        public void cancel() {
            cancelled = true;
        }

        public void doDownload(String downloadUrl) {
            InputStream input = null;
            FileOutputStream output = null;
            HttpURLConnection connection = null;
            boolean success = false;

            try {
                URL url = new URL(downloadUrl);
                connection = (HttpURLConnection) url.openConnection();
                
                // Set headers
                if (userAgent != null && !userAgent.isEmpty()) {
                    connection.setRequestProperty("User-Agent", userAgent);
                }
                connection.setRequestProperty("Accept", "*/*");
                connection.setRequestProperty("Accept-Encoding", "identity");
                
                connection.connect();

                // Check if response is valid
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    errorMessage = "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
                    onDownloadComplete(false);
                    return;
                }

                // Get file size
                int fileLength = connection.getContentLength();
                downloadItem.setFileSize(fileLength);
                
                // Update database with file size
                databaseHelper.updateDownload(downloadItem);

                // Download the file
                input = connection.getInputStream();
                output = new FileOutputStream(downloadItem.getFilePath());

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                
                while ((count = input.read(data)) != -1) {
                    // Check if task is cancelled
                    if (cancelled) {
                        onDownloadComplete(false);
                        return;
                    }
                    
                    total += count;
                    downloadItem.setDownloadedSize(total);
                    
                    // Update progress on main thread
                    final long currentTotal = total;
                    final int currentFileLength = fileLength;
                    mainHandler.post(() -> {
                        if (progressListener != null) {
                            progressListener.onDownloadProgress(downloadItem);
                        }
                        databaseHelper.updateDownload(downloadItem);
                    });
                    
                    output.write(data, 0, count);
                }

                success = true;
                
            } catch (Exception e) {
                errorMessage = e.getMessage();
                success = false;
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                } catch (IOException ignored) {}
                
                if (connection != null) connection.disconnect();
                
                onDownloadComplete(success);
            }
        }

        private void onDownloadComplete(boolean success) {
            mainHandler.post(() -> {
                activeDownloads.remove(downloadItem.getId());
                
                if (success && !cancelled) {
                    downloadItem.setStatus(DownloadItem.STATUS_COMPLETED);
                    databaseHelper.updateDownload(downloadItem);
                    
                    if (progressListener != null) {
                        progressListener.onDownloadCompleted(downloadItem);
                    }
                } else {
                    downloadItem.setStatus(DownloadItem.STATUS_FAILED);
                    databaseHelper.updateDownload(downloadItem);
                    
                    // Delete partial file
                    File file = new File(downloadItem.getFilePath());
                    if (file.exists()) {
                        file.delete();
                    }
                    
                    if (progressListener != null) {
                        String error = cancelled ? "Download cancelled" : errorMessage;
                        progressListener.onDownloadFailed(downloadItem, error);
                    }
                }
            });
        }
    }

    public void cleanup() {
        // Cancel all active downloads
        for (DownloadTask task : activeDownloads.values()) {
            task.cancel();
        }
        activeDownloads.clear();
        executorService.shutdown();
    }
}
