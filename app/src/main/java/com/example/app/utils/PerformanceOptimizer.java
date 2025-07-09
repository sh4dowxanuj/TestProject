package com.example.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performance optimization utility for WebView and general app performance
 */
public class PerformanceOptimizer {
    private static final String TAG = "PerformanceOptimizer";
    private static final int MEMORY_CACHE_SIZE = 1024 * 1024 * 8; // 8MB
    private static final int MAX_WEBVIEW_CACHE_SIZE = 1024 * 1024 * 50; // 50MB
    
    private static PerformanceOptimizer instance;
    private final LruCache<String, Bitmap> memoryCache;
    private final Map<String, String> domainPreloadCache;
    private final Context context;
    private final AtomicInteger activeWebViewCount;
    
    private PerformanceOptimizer(Context context) {
        this.context = context.getApplicationContext();
        this.domainPreloadCache = new ConcurrentHashMap<>();
        this.activeWebViewCount = new AtomicInteger(0);
        
        // Initialize bitmap cache
        this.memoryCache = new LruCache<String, Bitmap>(MEMORY_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };
    }
    
    public static synchronized PerformanceOptimizer getInstance(Context context) {
        if (instance == null) {
            instance = new PerformanceOptimizer(context);
        }
        return instance;
    }
    
    /**
     * Optimize WebView settings for better performance
     */
    public void optimizeWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        
        // JavaScript and DOM storage
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        
        // Caching strategy
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Note: setAppCacheEnabled and setAppCacheMaxSize are deprecated in API 33+
        // Modern WebView handles caching automatically
        
        // Performance optimizations
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setEnableSmoothTransition(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        
        // Hardware acceleration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        }
        
        // Network optimizations
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        
        // Text and layout optimizations
        settings.setTextZoom(100);
        settings.setDefaultTextEncodingName("utf-8");
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        
        // Security and privacy
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setGeolocationEnabled(false);
        settings.setSaveFormData(false);
        settings.setSavePassword(false);
        
        // Set user agent for better compatibility
        String userAgent = settings.getUserAgentString();
        if (userAgent != null && !userAgent.contains("Chrome")) {
            settings.setUserAgentString(userAgent + " Chrome/120.0.0.0");
        }
        
        // Media playback
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        // Zoom controls
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        
        activeWebViewCount.incrementAndGet();
    }
    
    /**
     * Clean up WebView resources
     */
    public void cleanupWebView(WebView webView) {
        if (webView != null) {
            webView.clearHistory();
            webView.clearCache(true);
            webView.clearFormData();
            webView.clearSslPreferences();
            webView.clearMatches();
            webView.clearFocus();
            activeWebViewCount.decrementAndGet();
        }
    }
    
    /**
     * Get bitmap from cache
     */
    public Bitmap getBitmapFromCache(String key) {
        return memoryCache.get(key);
    }
    
    /**
     * Add bitmap to cache
     */
    public void addBitmapToCache(String key, Bitmap bitmap) {
        if (getBitmapFromCache(key) == null && bitmap != null) {
            memoryCache.put(key, bitmap);
        }
    }
    
    /**
     * Clear image cache
     */
    public void clearImageCache() {
        memoryCache.evictAll();
    }
    
    /**
     * Get memory usage statistics
     */
    public MemoryStats getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return new MemoryStats(usedMemory, totalMemory, maxMemory, activeWebViewCount.get());
    }
    
    /**
     * Force garbage collection (use sparingly)
     */
    public void forceGarbageCollection() {
        System.gc();
    }
    
    /**
     * Clear WebView cache directory
     */
    public void clearWebViewCache() {
        try {
            File cacheDir = new File(context.getCacheDir(), "webview");
            if (cacheDir.exists()) {
                deleteRecursively(cacheDir);
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }
    
    /**
     * Preload domain for faster navigation
     */
    public void preloadDomain(String domain) {
        if (domain != null && !domainPreloadCache.containsKey(domain)) {
            domainPreloadCache.put(domain, System.currentTimeMillis() + "");
        }
    }
    
    /**
     * Check if domain is preloaded
     */
    public boolean isDomainPreloaded(String domain) {
        return domainPreloadCache.containsKey(domain);
    }
    
    /**
     * Clean up preload cache
     */
    public void cleanupPreloadCache() {
        long currentTime = System.currentTimeMillis();
        domainPreloadCache.entrySet().removeIf(entry -> {
            try {
                long loadTime = Long.parseLong(entry.getValue());
                return currentTime - loadTime > 300000; // 5 minutes
            } catch (NumberFormatException e) {
                return true;
            }
        });
    }
    
    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
    
    /**
     * Memory statistics class
     */
    public static class MemoryStats {
        public final long usedMemory;
        public final long totalMemory;
        public final long maxMemory;
        public final int activeWebViews;
        
        public MemoryStats(long usedMemory, long totalMemory, long maxMemory, int activeWebViews) {
            this.usedMemory = usedMemory;
            this.totalMemory = totalMemory;
            this.maxMemory = maxMemory;
            this.activeWebViews = activeWebViews;
        }
        
        public double getUsagePercentage() {
            return (double) usedMemory / maxMemory * 100;
        }
        
        public String getFormattedUsage() {
            return String.format("%.1f%% (%d/%d MB)", 
                getUsagePercentage(), 
                usedMemory / (1024 * 1024), 
                maxMemory / (1024 * 1024));
        }
    }
}
