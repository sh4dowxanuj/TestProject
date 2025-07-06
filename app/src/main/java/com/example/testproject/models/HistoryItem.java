package com.example.testproject.models;

public class HistoryItem {
    private long id;
    private String title;
    private String url;
    private long timestamp;

    public HistoryItem() {}

    public HistoryItem(String title, String url) {
        this.title = title;
        this.url = url;
        this.timestamp = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
