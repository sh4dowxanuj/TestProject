package com.example.testproject.models;

public enum SearchEngine {
    GOOGLE("Google", "https://www.google.com/search?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p="),
    YANDEX("Yandex", "https://yandex.com/search/?text="),
    BAIDU("Baidu", "https://www.baidu.com/s?wd=");

    private final String name;
    private final String searchUrl;

    SearchEngine(String name, String searchUrl) {
        this.name = name;
        this.searchUrl = searchUrl;
    }

    public String getName() {
        return name;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public static SearchEngine fromName(String name) {
        for (SearchEngine engine : values()) {
            if (engine.getName().equals(name)) {
                return engine;
            }
        }
        return GOOGLE; // Default fallback
    }

    public static SearchEngine fromOrdinal(int ordinal) {
        SearchEngine[] engines = values();
        if (ordinal >= 0 && ordinal < engines.length) {
            return engines[ordinal];
        }
        return GOOGLE; // Default fallback
    }
}
