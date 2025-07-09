package com.example.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.os.Build;

/**
 * Theme and appearance management utility
 */
public class ThemeManager {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_THEME_COLOR = "theme_color";
    private static final String KEY_FONT_SIZE = "font_size";
    
    public enum ThemeMode {
        LIGHT, DARK, SYSTEM
    }
    
    public enum ThemeColor {
        BLUE("#2196F3"),
        GREEN("#4CAF50"),
        RED("#F44336"),
        PURPLE("#9C27B0"),
        ORANGE("#FF9800"),
        TEAL("#009688");
        
        private final String colorCode;
        
        ThemeColor(String colorCode) {
            this.colorCode = colorCode;
        }
        
        public String getColorCode() {
            return colorCode;
        }
    }
    
    private static ThemeManager instance;
    private final SharedPreferences prefs;
    private final Context context;
    
    private ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }
    
    /**
     * Set dark mode preference
     */
    public void setDarkMode(ThemeMode mode) {
        prefs.edit().putString(KEY_DARK_MODE, mode.name()).apply();
    }
    
    /**
     * Get dark mode preference
     */
    public ThemeMode getDarkMode() {
        String mode = prefs.getString(KEY_DARK_MODE, ThemeMode.SYSTEM.name());
        try {
            return ThemeMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            return ThemeMode.SYSTEM;
        }
    }
    
    /**
     * Check if dark mode is currently active
     */
    public boolean isDarkModeActive() {
        ThemeMode mode = getDarkMode();
        if (mode == ThemeMode.SYSTEM) {
            return (context.getResources().getConfiguration().uiMode & 
                   android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                   android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
        return mode == ThemeMode.DARK;
    }
    
    /**
     * Set theme color
     */
    public void setThemeColor(ThemeColor color) {
        prefs.edit().putString(KEY_THEME_COLOR, color.name()).apply();
    }
    
    /**
     * Get theme color
     */
    public ThemeColor getThemeColor() {
        String color = prefs.getString(KEY_THEME_COLOR, ThemeColor.BLUE.name());
        try {
            return ThemeColor.valueOf(color);
        } catch (IllegalArgumentException e) {
            return ThemeColor.BLUE;
        }
    }
    
    /**
     * Set font size preference
     */
    public void setFontSize(int size) {
        prefs.edit().putInt(KEY_FONT_SIZE, size).apply();
    }
    
    /**
     * Get font size preference
     */
    public int getFontSize() {
        return prefs.getInt(KEY_FONT_SIZE, 16);
    }
    
    /**
     * Apply theme to WebView
     */
    public void applyThemeToWebView(WebView webView) {
        if (webView == null) return;
        
        WebSettings settings = webView.getSettings();
        
        // Apply font size
        settings.setDefaultFontSize(getFontSize());
        
        // Apply dark mode CSS if enabled
        if (isDarkModeActive()) {
            String darkModeCSS = "javascript:(function() {" +
                "var css = 'html { filter: invert(1) hue-rotate(180deg) !important; }' +" +
                "'img, video, iframe, svg, embed, object { filter: invert(1) hue-rotate(180deg) !important; }';" +
                "var style = document.createElement('style');" +
                "style.textContent = css;" +
                "document.head.appendChild(style);" +
                "})()";
            webView.evaluateJavascript(darkModeCSS, null);
        }
    }
    
    /**
     * Get CSS for dark mode
     */
    public String getDarkModeCSS() {
        return "html { " +
               "filter: invert(1) hue-rotate(180deg) !important; " +
               "background: #121212 !important; " +
               "color: #ffffff !important; " +
               "} " +
               "img, video, iframe, svg, embed, object { " +
               "filter: invert(1) hue-rotate(180deg) !important; " +
               "} " +
               "a { color: #bb86fc !important; } " +
               "a:visited { color: #9c27b0 !important; }";
    }
}
