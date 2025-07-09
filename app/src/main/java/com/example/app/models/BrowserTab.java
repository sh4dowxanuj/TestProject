package com.example.app.models;

import android.webkit.WebView;

public class BrowserTab {
    private String title;
    private String url;
    private WebView webView;
    private boolean isSelected;
    private boolean isPrivate;

    public BrowserTab(String title, String url) {
        this.title = title;
        this.url = url;
        this.isSelected = false;
        this.isPrivate = false;
    }

    public BrowserTab(String title, String url, boolean isPrivate) {
        this.title = title;
        this.url = url;
        this.isSelected = false;
        this.isPrivate = isPrivate;
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

    public WebView getWebView() {
        return webView;
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}
