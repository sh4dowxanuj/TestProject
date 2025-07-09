package com.example.app.utils;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.webkit.URLUtil;

import com.example.app.database.DatabaseHelper;
import com.example.app.models.DownloadItem;

import java.io.BufferedOutputStream;
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

public class WebDownloader {
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

    public WebDownloader(Context context, DatabaseHelper databaseHelper) {
        this.context = context;
        this.databaseHelper = databaseHelper;
        this.activeDownloads = new HashMap<>();
        // Optimize thread pool for better performance
        this.executorService = Executors.newFixedThreadPool(2); // Reduced to 2 to avoid overwhelming the system
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
    
    public void cancelAllDownloads() {
        for (DownloadTask task : activeDownloads.values()) {
            task.cancel();
        }
        activeDownloads.clear();
    }

    public boolean isDownloading(long downloadId) {
        return activeDownloads.containsKey(downloadId);
    }

    private class DownloadTask {
        private DownloadItem downloadItem;
        private String userAgent;
        private String errorMessage;
        private volatile boolean cancelled = false;
        private long lastProgressUpdate = 0;
        private long lastProgressBytes = 0;
        private static final long PROGRESS_UPDATE_INTERVAL = 200; // Update every 200ms (reduced frequency)
        private long minProgressBytes = 65536; // Dynamic minimum bytes (64KB default)

        public DownloadTask(DownloadItem downloadItem, String userAgent) {
            this.downloadItem = downloadItem;
            this.userAgent = userAgent;
        }

        public void cancel() {
            cancelled = true;
        }

        public void doDownload(String downloadUrl) {
            InputStream input = null;
            BufferedOutputStream output = null;
            HttpURLConnection connection = null;
            boolean success = false;

            try {
                URL url = new URL(downloadUrl);
                connection = (HttpURLConnection) url.openConnection();
                
                // Set connection timeouts
                connection.setConnectTimeout(15000); // 15 seconds
                connection.setReadTimeout(30000); // 30 seconds
                
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
                
                // Set dynamic progress update threshold based on file size
                if (fileLength > 0) {
                    // For large files, update less frequently to avoid UI lag
                    if (fileLength > 100 * 1024 * 1024) { // Files larger than 100MB
                        minProgressBytes = 1024 * 1024; // Update every 1MB
                    } else if (fileLength > 10 * 1024 * 1024) { // Files larger than 10MB
                        minProgressBytes = 256 * 1024; // Update every 256KB
                    } else {
                        minProgressBytes = 65536; // Update every 64KB for smaller files
                    }
                }
                
                // Update database with file size
                databaseHelper.updateDownload(downloadItem);

                // Download the file with buffered output
                input = connection.getInputStream();
                output = new BufferedOutputStream(new FileOutputStream(downloadItem.getFilePath()));

                // Use adaptive buffer size based on file size
                int bufferSize = 8192; // Default 8KB
                if (fileLength > 10 * 1024 * 1024) { // Files larger than 10MB
                    bufferSize = 16384; // Use 16KB buffer
                } else if (fileLength > 50 * 1024 * 1024) { // Files larger than 50MB
                    bufferSize = 32768; // Use 32KB buffer
                }
                
                byte[] data = new byte[bufferSize];
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
                    
                    // Only update progress if enough time has passed or enough bytes downloaded
                    long currentTime = System.currentTimeMillis();
                    boolean shouldUpdate = false;
                    
                    if (currentTime - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL ||
                        total - lastProgressBytes >= minProgressBytes ||
                        (fileLength > 0 && total == fileLength)) { // Always update on completion if size known
                        
                        shouldUpdate = true;
                        lastProgressUpdate = currentTime;
                        lastProgressBytes = total;
                    }
                    
                    if (shouldUpdate) {
                        // Update progress on main thread with throttling
                        mainHandler.post(() -> {
                            if (progressListener != null && !cancelled) {
                                progressListener.onDownloadProgress(downloadItem);
                            }
                            // Use lighter database update for progress
                            databaseHelper.updateDownloadProgress(downloadItem);
                        });
                    }
                    
                    output.write(data, 0, count);
                }

                // Flush any remaining data
                output.flush();
                
                // Final progress update to ensure 100% completion
                mainHandler.post(() -> {
                    if (progressListener != null && !cancelled) {
                        progressListener.onDownloadProgress(downloadItem);
                    }
                    databaseHelper.updateDownload(downloadItem);
                });
                
                success = true;
                
            } catch (IOException e) {
                errorMessage = "Network error: " + e.getMessage();
                success = false;
            } catch (Exception e) {
                errorMessage = "Download error: " + e.getMessage();
                success = false;
            } finally {
                try {
                    if (output != null) {
                        output.flush();
                        output.close();
                    }
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
