package com.example.app.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.app.database.DatabaseHelper;
import com.example.app.models.Bookmark;
import com.example.app.models.HistoryItem;
import com.example.app.models.SearchSuggestion;

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
        this.executorService = Executors.newFixedThreadPool(2); // Use fixed thread pool for better resource management
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
                // Check if thread is interrupted before processing
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                
                List<SearchSuggestion> suggestions = generateSuggestions(query.trim());
                
                // Check again before posting result
                if (!Thread.currentThread().isInterrupted()) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuggestionsReady(suggestions);
                        }
                    });
                }
            }
        });
    }
    
    private List<SearchSuggestion> generateSuggestions(String query) {
        List<SearchSuggestion> suggestions = new ArrayList<>();
        
        try {
            // Check if thread is interrupted
            if (Thread.currentThread().isInterrupted()) {
                return suggestions;
            }
            
            // Add history suggestions
            List<HistoryItem> historyItems = databaseHelper.searchHistory(query, MAX_HISTORY_SUGGESTIONS);
            for (HistoryItem item : historyItems) {
                suggestions.add(new SearchSuggestion(item.getTitle() + " - " + item.getUrl(), 
                    SearchSuggestion.SuggestionType.HISTORY));
            }
            
            // Check if thread is interrupted
            if (Thread.currentThread().isInterrupted()) {
                return suggestions;
            }
            
            // Add bookmark suggestions
            List<Bookmark> bookmarks = databaseHelper.searchBookmarks(query, MAX_BOOKMARK_SUGGESTIONS);
            for (Bookmark bookmark : bookmarks) {
                suggestions.add(new SearchSuggestion(bookmark.getTitle() + " - " + bookmark.getUrl(), 
                    SearchSuggestion.SuggestionType.BOOKMARK));
            }
            
            // Add online search suggestions if we haven't reached the limit
            if (suggestions.size() < MAX_SUGGESTIONS && !Thread.currentThread().isInterrupted()) {
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
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        
        try {
            // Check if thread is interrupted before making network request
            if (Thread.currentThread().isInterrupted()) {
                return suggestions;
            }
            
            // Use Google's autocomplete API
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String urlString = "http://suggestqueries.google.com/complete/search?client=firefox&q=" + encodedQuery;
            
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            
            // Check if thread is interrupted before reading response
            if (Thread.currentThread().isInterrupted()) {
                return suggestions;
            }
            
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                // Check if thread is interrupted during reading
                if (Thread.currentThread().isInterrupted()) {
                    return suggestions;
                }
                response.append(line);
            }
            
            // Parse JSON response
            JSONArray jsonArray = new JSONArray(response.toString());
            if (jsonArray.length() > 1) {
                JSONArray suggestionsArray = jsonArray.getJSONArray(1);
                int count = Math.min(suggestionsArray.length(), maxResults);
                
                for (int i = 0; i < count; i++) {
                    // Check if thread is interrupted during parsing
                    if (Thread.currentThread().isInterrupted()) {
                        return suggestions;
                    }
                    String suggestion = suggestionsArray.getString(i);
                    suggestions.add(new SearchSuggestion(suggestion, SearchSuggestion.SuggestionType.SEARCH_QUERY));
                }
            }
            
        } catch (Exception e) {
            // If online suggestions fail, add some basic suggestions
            if (query.length() > 1) {
                suggestions.add(new SearchSuggestion(query, SearchSuggestion.SuggestionType.SEARCH_QUERY));
            }
        } finally {
            // Properly close resources
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // Ignore close exceptions
                }
            }
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    // Ignore disconnect exceptions
                }
            }
        }
        
        return suggestions;
    }
    
    public void cancelPendingRequests() {
        if (executorService != null && !executorService.isShutdown()) {
            // Shutdown executor to cancel pending tasks and recreate for better memory management
            List<Runnable> unfinishedTasks = executorService.shutdownNow();
            executorService = Executors.newFixedThreadPool(2); // Use fixed thread pool for better resource management
        }
    }
    
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // Force shutdown and interrupt running tasks
            try {
                // Wait for tasks to terminate
                if (!executorService.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    // Force shutdown if tasks didn't terminate
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                // Re-interrupt the thread
                Thread.currentThread().interrupt();
                executorService.shutdownNow();
            }
        }
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
