package com.example.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebViewDatabase;
import android.os.Build;

/**
 * Private browsing mode (Incognito) management - Chrome-like tab-based implementation
 */
public class PrivateBrowsingManager {
    private static final String PREFS_NAME = "private_browsing_prefs";
    
    private static PrivateBrowsingManager instance;
    private final Context context;
    private int privateTabCount = 0;
    
    private PrivateBrowsingManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized PrivateBrowsingManager getInstance(Context context) {
        if (instance == null) {
            instance = new PrivateBrowsingManager(context);
        }
        return instance;
    }
    
    /**
     * Increment private tab count
     */
    public void addPrivateTab() {
        privateTabCount++;
    }
    
    /**
     * Decrement private tab count and cleanup if no private tabs remain
     */
    public void removePrivateTab() {
        privateTabCount--;
        if (privateTabCount <= 0) {
            privateTabCount = 0;
            clearPrivateData();
        }
    }
    
    /**
     * Check if any private tabs are open
     */
    public boolean hasPrivateTabs() {
        return privateTabCount > 0;
    }
    
    /**
     * Get count of private tabs
     */
    public int getPrivateTabCount() {
        return privateTabCount;
    }
    
    /**
     * Configure WebView for private browsing (called for each private tab)
     */
    public void configureWebViewForPrivateMode(WebView webView, boolean isPrivate) {
        if (webView == null) return;
        
        WebSettings settings = webView.getSettings();
        
        if (isPrivate) {
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
    public String getPrivateModeIndicator(boolean isPrivate) {
        return isPrivate ? "🔒 Private" : "🌐 Normal";
    }
    
    /**
     * Should save history for this tab
     */
    public boolean shouldSaveHistory(boolean isPrivate) {
        return !isPrivate;
    }
    
    /**
     * Should save downloads for this tab
     */
    public boolean shouldSaveDownloads(boolean isPrivate) {
        return !isPrivate;
    }
    
    /**
     * Should save form data for this tab
     */
    public boolean shouldSaveFormData(boolean isPrivate) {
        return !isPrivate;
    }
    
    /**
     * Should save cookies for this tab
     */
    public boolean shouldSaveCookies(boolean isPrivate) {
        return !isPrivate;
    }
}
