package com.example.testproject.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.testproject.database.DatabaseHelper;
import com.example.testproject.models.Bookmark;
import com.example.testproject.models.HistoryItem;
import com.example.testproject.models.SearchSuggestion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;

public class SearchSuggestionProvider {
    private static final int MAX_SUGGESTIONS = 8;
    private static final int MAX_HISTORY_SUGGESTIONS = 3;
    private static final int MAX_BOOKMARK_SUGGESTIONS = 2;
    private static final int MAX_ONLINE_SUGGESTIONS = 5;
    
    private DatabaseHelper databaseHelper;
    private Context context;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    public interface SuggestionCallback {
        void onSuggestionsReady(List<SearchSuggestion> suggestions);
    }
    
    public SearchSuggestionProvider(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public void getSuggestions(String query, SuggestionCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onSuggestionsReady(new ArrayList<>());
            return;
        }
        
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                List<SearchSuggestion> suggestions = generateSuggestions(query.trim());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuggestionsReady(suggestions);
                    }
                });
            }
        });
    }
    
    private List<SearchSuggestion> generateSuggestions(String query) {
        List<SearchSuggestion> suggestions = new ArrayList<>();
        
        try {
            // Add history suggestions
            List<HistoryItem> historyItems = databaseHelper.searchHistory(query, MAX_HISTORY_SUGGESTIONS);
            for (HistoryItem item : historyItems) {
                suggestions.add(new SearchSuggestion(item.getTitle() + " - " + item.getUrl(), 
                    SearchSuggestion.SuggestionType.HISTORY));
            }
            
            // Add bookmark suggestions
            List<Bookmark> bookmarks = databaseHelper.searchBookmarks(query, MAX_BOOKMARK_SUGGESTIONS);
            for (Bookmark bookmark : bookmarks) {
                suggestions.add(new SearchSuggestion(bookmark.getTitle() + " - " + bookmark.getUrl(), 
                    SearchSuggestion.SuggestionType.BOOKMARK));
            }
            
            // Add online search suggestions if we haven't reached the limit
            if (suggestions.size() < MAX_SUGGESTIONS) {
                List<SearchSuggestion> onlineSuggestions = getOnlineSearchSuggestions(query, 
                    MAX_SUGGESTIONS - suggestions.size());
                suggestions.addAll(onlineSuggestions);
            }
        } catch (Exception e) {
            // If there's an error, at least return the query as a suggestion
            suggestions.add(new SearchSuggestion(query, SearchSuggestion.SuggestionType.SEARCH_QUERY));
        }
        
        return suggestions;
    }
    
    private List<SearchSuggestion> getOnlineSearchSuggestions(String query, int maxResults) {
        List<SearchSuggestion> suggestions = new ArrayList<>();
        
        try {
            // Use Google's autocomplete API
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String urlString = "http://suggestqueries.google.com/complete/search?client=firefox&q=" + encodedQuery;
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse JSON response
            JSONArray jsonArray = new JSONArray(response.toString());
            if (jsonArray.length() > 1) {
                JSONArray suggestionsArray = jsonArray.getJSONArray(1);
                int count = Math.min(suggestionsArray.length(), maxResults);
                
                for (int i = 0; i < count; i++) {
                    String suggestion = suggestionsArray.getString(i);
                    suggestions.add(new SearchSuggestion(suggestion, SearchSuggestion.SuggestionType.SEARCH_QUERY));
                }
            }
            
        } catch (Exception e) {
            // If online suggestions fail, add some basic suggestions
            if (query.length() > 1) {
                suggestions.add(new SearchSuggestion(query, SearchSuggestion.SuggestionType.SEARCH_QUERY));
            }
        }
        
        return suggestions;
    }
    
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
