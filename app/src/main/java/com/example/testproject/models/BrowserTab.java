package com.example.testproject.models;

import android.webkit.WebView;

public class BrowserTab {
    private String title;
    private String url;
    private WebView webView;
    private boolean isSelected;

    public BrowserTab(String title, String url) {
        this.title = title;
        this.url = url;
        this.isSelected = false;
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
}
