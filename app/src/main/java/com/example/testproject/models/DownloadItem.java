package com.example.testproject.models;

public class DownloadItem {
    private long id;
    private String title;
    private String url;
    private String fileName;
    private String filePath;
    private long fileSize;
    private long downloadedSize;
    private int status; // 0: downloading, 1: completed, 2: failed, 3: paused
    private long timestamp;
    private String mimeType;

    // Status constants
    public static final int STATUS_DOWNLOADING = 0;
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_FAILED = 2;
    public static final int STATUS_PAUSED = 3;

    public DownloadItem() {}

    public DownloadItem(String title, String url, String fileName, String filePath, 
                       long fileSize, String mimeType) {
        this.title = title;
        this.url = url;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.downloadedSize = 0;
        this.status = STATUS_DOWNLOADING;
        this.timestamp = System.currentTimeMillis();
        this.mimeType = mimeType;
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public long getDownloadedSize() { return downloadedSize; }
    public void setDownloadedSize(long downloadedSize) { this.downloadedSize = downloadedSize; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public int getProgress() {
        if (fileSize == 0) return 0;
        return (int) ((downloadedSize * 100) / fileSize);
    }

    public String getStatusText() {
        switch (status) {
            case STATUS_DOWNLOADING: return "Downloading";
            case STATUS_COMPLETED: return "Completed";
            case STATUS_FAILED: return "Failed";
            case STATUS_PAUSED: return "Paused";
            default: return "Unknown";
        }
    }
}
