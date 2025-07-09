package com.example.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;

/**
 * User Agent management utility
 */
public class UserAgentManager {
    private static final String PREFS_NAME = "user_agent_prefs";
    private static final String KEY_SELECTED_USER_AGENT = "selected_user_agent";
    private static final String KEY_CUSTOM_USER_AGENT = "custom_user_agent";
    
    public enum UserAgentType {
        DEFAULT("Default", ""),
        CHROME_DESKTOP("Chrome Desktop", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"),
        CHROME_MOBILE("Chrome Mobile", "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"),
        FIREFOX_DESKTOP("Firefox Desktop", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0"),
        FIREFOX_MOBILE("Firefox Mobile", "Mozilla/5.0 (Mobile; rv:109.0) Gecko/109.0 Firefox/121.0"),
        SAFARI_DESKTOP("Safari Desktop", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"),
        SAFARI_MOBILE("Safari Mobile", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"),
        EDGE_DESKTOP("Edge Desktop", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"),
        OPERA_DESKTOP("Opera Desktop", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 OPR/106.0.0.0"),
        GOOGLEBOT("Googlebot", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"),
        CUSTOM("Custom", "");
        
        private final String displayName;
        private final String userAgent;
        
        UserAgentType(String displayName, String userAgent) {
            this.displayName = displayName;
            this.userAgent = userAgent;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getUserAgent() {
            return userAgent;
        }
    }
    
    private static UserAgentManager instance;
    private final SharedPreferences prefs;
    private final Context context;
    private final Map<String, String> deviceSpecificUserAgents;
    
    private UserAgentManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.deviceSpecificUserAgents = new HashMap<>();
        initializeDeviceSpecificUserAgents();
    }
    
    public static synchronized UserAgentManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserAgentManager(context);
        }
        return instance;
    }
    
    /**
     * Initialize device-specific user agents
     */
    private void initializeDeviceSpecificUserAgents() {
        String androidVersion = Build.VERSION.RELEASE;
        String deviceModel = Build.MODEL;
        String deviceManufacturer = Build.MANUFACTURER;
        
        // Create device-specific Chrome Mobile user agent
        String chromeVersion = "120.0.0.0";
        String chromeMobileUA = String.format(
            "Mozilla/5.0 (Linux; Android %s; %s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s Mobile Safari/537.36",
            androidVersion, deviceModel, chromeVersion
        );
        
        deviceSpecificUserAgents.put("CHROME_MOBILE_DEVICE", chromeMobileUA);
        
        // Create device-specific Firefox Mobile user agent
        String firefoxMobileUA = String.format(
            "Mozilla/5.0 (Mobile; rv:109.0) Gecko/109.0 Firefox/121.0",
            androidVersion
        );
        
        deviceSpecificUserAgents.put("FIREFOX_MOBILE_DEVICE", firefoxMobileUA);
    }
    
    /**
     * Set selected user agent type
     */
    public void setUserAgentType(UserAgentType type) {
        prefs.edit().putString(KEY_SELECTED_USER_AGENT, type.name()).apply();
    }
    
    /**
     * Get selected user agent type
     */
    public UserAgentType getUserAgentType() {
        String type = prefs.getString(KEY_SELECTED_USER_AGENT, UserAgentType.DEFAULT.name());
        try {
            return UserAgentType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return UserAgentType.DEFAULT;
        }
    }
    
    /**
     * Set custom user agent
     */
    public void setCustomUserAgent(String userAgent) {
        prefs.edit().putString(KEY_CUSTOM_USER_AGENT, userAgent).apply();
    }
    
    /**
     * Get custom user agent
     */
    public String getCustomUserAgent() {
        return prefs.getString(KEY_CUSTOM_USER_AGENT, "");
    }
    
    /**
     * Get current user agent string
     */
    public String getCurrentUserAgent(WebView webView) {
        UserAgentType type = getUserAgentType();
        
        switch (type) {
            case DEFAULT:
                return webView != null ? webView.getSettings().getUserAgentString() : "";
            case CHROME_MOBILE:
                return deviceSpecificUserAgents.get("CHROME_MOBILE_DEVICE");
            case FIREFOX_MOBILE:
                return deviceSpecificUserAgents.get("FIREFOX_MOBILE_DEVICE");
            case CUSTOM:
                return getCustomUserAgent();
            default:
                return type.getUserAgent();
        }
    }
    
    /**
     * Apply user agent to WebView
     */
    public void applyUserAgent(WebView webView) {
        if (webView == null) return;
        
        String userAgent = getCurrentUserAgent(webView);
        if (!userAgent.isEmpty()) {
            webView.getSettings().setUserAgentString(userAgent);
        }
    }
    
    /**
     * Get all available user agent types
     */
    public UserAgentType[] getAllUserAgentTypes() {
        return UserAgentType.values();
    }
    
    /**
     * Get user agent for specific type
     */
    public String getUserAgentForType(UserAgentType type, WebView webView) {
        switch (type) {
            case DEFAULT:
                return webView != null ? webView.getSettings().getUserAgentString() : "";
            case CHROME_MOBILE:
                return deviceSpecificUserAgents.get("CHROME_MOBILE_DEVICE");
            case FIREFOX_MOBILE:
                return deviceSpecificUserAgents.get("FIREFOX_MOBILE_DEVICE");
            case CUSTOM:
                return getCustomUserAgent();
            default:
                return type.getUserAgent();
        }
    }
    
    /**
     * Check if user agent is mobile
     */
    public boolean isMobileUserAgent() {
        UserAgentType type = getUserAgentType();
        return type == UserAgentType.CHROME_MOBILE || 
               type == UserAgentType.FIREFOX_MOBILE || 
               type == UserAgentType.SAFARI_MOBILE;
    }
    
    /**
     * Check if user agent is desktop
     */
    public boolean isDesktopUserAgent() {
        UserAgentType type = getUserAgentType();
        return type == UserAgentType.CHROME_DESKTOP || 
               type == UserAgentType.FIREFOX_DESKTOP || 
               type == UserAgentType.SAFARI_DESKTOP ||
               type == UserAgentType.EDGE_DESKTOP ||
               type == UserAgentType.OPERA_DESKTOP;
    }
    
    /**
     * Rotate to next user agent
     */
    public void rotateUserAgent() {
        UserAgentType[] types = UserAgentType.values();
        UserAgentType current = getUserAgentType();
        
        for (int i = 0; i < types.length; i++) {
            if (types[i] == current) {
                UserAgentType next = types[(i + 1) % types.length];
                setUserAgentType(next);
                break;
            }
        }
    }
}
