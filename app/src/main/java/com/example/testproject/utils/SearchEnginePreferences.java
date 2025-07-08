package com.example.testproject.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.testproject.models.SearchEngine;

public class SearchEnginePreferences {
    private static final String PREFS_NAME = "search_engine_prefs";
    private static final String KEY_SEARCH_ENGINE = "selected_search_engine";
    private static final int DEFAULT_SEARCH_ENGINE = 0; // Google

    private final SharedPreferences prefs;

    public SearchEnginePreferences(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public SearchEngine getSelectedSearchEngine() {
        int ordinal = prefs.getInt(KEY_SEARCH_ENGINE, DEFAULT_SEARCH_ENGINE);
        return SearchEngine.fromOrdinal(ordinal);
    }

    public void setSelectedSearchEngine(SearchEngine searchEngine) {
        prefs.edit()
             .putInt(KEY_SEARCH_ENGINE, searchEngine.ordinal())
             .apply();
    }

    public String[] getSearchEngineNames() {
        SearchEngine[] engines = SearchEngine.values();
        String[] names = new String[engines.length];
        for (int i = 0; i < engines.length; i++) {
            names[i] = engines[i].getName();
        }
        return names;
    }
}
