package com.example.app.models;

public class SearchSuggestion {
    private String query;
    private SuggestionType type;
    
    public enum SuggestionType {
        SEARCH_QUERY,
        URL,
        HISTORY,
        BOOKMARK
    }
    
    public SearchSuggestion(String query, SuggestionType type) {
        this.query = query;
        this.type = type;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public SuggestionType getType() {
        return type;
    }
    
    public void setType(SuggestionType type) {
        this.type = type;
    }
}
