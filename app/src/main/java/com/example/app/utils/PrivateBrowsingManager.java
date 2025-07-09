package com.example.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebViewDatabase;
import android.os.Build;

/**
 * Private browsing mode (Incognito) management
 */
public class PrivateBrowsingManager {
    private static final String PREFS_NAME = "private_browsing_prefs";
    private static final String KEY_PRIVATE_MODE = "private_mode";
    
    private static PrivateBrowsingManager instance;
    private final SharedPreferences prefs;
    private final Context context;
    private boolean isPrivateMode = false;
    
    private PrivateBrowsingManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized PrivateBrowsingManager getInstance(Context context) {
        if (instance == null) {
            instance = new PrivateBrowsingManager(context);
        }
        return instance;
    }
    
    /**
     * Enable private browsing mode
     */
    public void enablePrivateMode() {
        isPrivateMode = true;
        prefs.edit().putBoolean(KEY_PRIVATE_MODE, true).apply();
    }
    
    /**
     * Disable private browsing mode
     */
    public void disablePrivateMode() {
        isPrivateMode = false;
        prefs.edit().putBoolean(KEY_PRIVATE_MODE, false).apply();
        clearPrivateData();
    }
    
    /**
     * Check if private mode is enabled
     */
    public boolean isPrivateModeEnabled() {
        return isPrivateMode || prefs.getBoolean(KEY_PRIVATE_MODE, false);
    }
    
    /**
     * Configure WebView for private browsing
     */
    public void configureWebViewForPrivateMode(WebView webView) {
        if (webView == null) return;
        
        WebSettings settings = webView.getSettings();
        
        if (isPrivateModeEnabled()) {
            // Disable data storage
            settings.setDomStorageEnabled(false);
            settings.setDatabaseEnabled(false);
            // Note: setAppCacheEnabled is deprecated in API 33+
            settings.setSaveFormData(false);
            settings.setSavePassword(false);
            settings.setGeolocationEnabled(false);
            
            // Set cache mode to no cache
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            
            // Clear existing data
            webView.clearHistory();
            webView.clearCache(true);
            webView.clearFormData();
            webView.clearSslPreferences();
            webView.clearMatches();
            
            // Additional privacy settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            }
            
            // Disable location services
            settings.setGeolocationEnabled(false);
            
            // Remove referer header
            settings.setUserAgentString(settings.getUserAgentString() + " (Private)");
            
        } else {
            // Normal browsing mode
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            // Note: setAppCacheEnabled is deprecated in API 33+
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            
            // Reset user agent
            String userAgent = settings.getUserAgentString();
            if (userAgent.contains(" (Private)")) {
                settings.setUserAgentString(userAgent.replace(" (Private)", ""));
            }
        }
    }
    
    /**
     * Clear all private browsing data
     */
    public void clearPrivateData() {
        try {
            // Clear WebView data
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                WebStorage.getInstance().deleteAllData();
            }
            
            // Clear WebView database
            WebViewDatabase.getInstance(context).clearFormData();
            WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword();
            
            // Clear cookies in private mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.webkit.CookieManager.getInstance().removeAllCookies(null);
            } else {
                android.webkit.CookieManager.getInstance().removeAllCookie();
            }
            
        } catch (Exception e) {
            // Ignore errors
        }
    }
    
    /**
     * Get private mode indicator text
     */
    public String getPrivateModeIndicator() {
        return isPrivateModeEnabled() ? "🔒 Private" : "🌐 Normal";
    }
    
    /**
     * Should save history
     */
    public boolean shouldSaveHistory() {
        return !isPrivateModeEnabled();
    }
    
    /**
     * Should save downloads
     */
    public boolean shouldSaveDownloads() {
        return !isPrivateModeEnabled();
    }
    
    /**
     * Should save form data
     */
    public boolean shouldSaveFormData() {
        return !isPrivateModeEnabled();
    }
    
    /**
     * Should save cookies
     */
    public boolean shouldSaveCookies() {
        return !isPrivateModeEnabled();
    }
}
