package com.example.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebStorage;
import android.webkit.WebViewDatabase;
import android.os.Handler;
import android.os.Looper;

import com.example.app.utils.PerformanceOptimizer;

public class MemoryOptimizedActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    
    private static final int MEMORY_CLEANUP_DELAY = 5000; // 5 seconds
    private Handler cleanupHandler = new Handler(Looper.getMainLooper());
    
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // Activity created - initialize performance optimizer
        if (activity instanceof MainActivity) {
            PerformanceOptimizer.getInstance(activity);
        }
    }
    
    @Override
    public void onActivityStarted(Activity activity) {
        // Activity started - cancel any pending cleanup
        cleanupHandler.removeCallbacksAndMessages(null);
    }
    
    @Override
    public void onActivityResumed(Activity activity) {
        // Activity resumed - preload common resources
        if (activity instanceof MainActivity) {
            PerformanceOptimizer optimizer = PerformanceOptimizer.getInstance(activity);
            optimizer.cleanupPreloadCache();
        }
    }
    
    @Override
    public void onActivityPaused(Activity activity) {
        // Activity paused - schedule memory cleanup
        cleanupHandler.postDelayed(() -> {
            performMemoryCleanup();
        }, MEMORY_CLEANUP_DELAY);
    }
    
    @Override
    public void onActivityStopped(Activity activity) {
        // Activity stopped - immediate light cleanup
        performLightCleanup();
    }
    
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // Activity saving instance state
    }
    
    @Override
    public void onActivityDestroyed(Activity activity) {
        // Activity destroyed - cleanup WebViews and other resources
        if (activity instanceof MainActivity) {
            performDeepCleanup();
        }
    }
    
    private void performLightCleanup() {
        // Light cleanup - no GC calls
        try {
            // Clear temporary caches
            System.runFinalization();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    private void performMemoryCleanup() {
        try {
            // Clear WebView caches
            WebStorage.getInstance().deleteAllData();
            
            // Clear image caches
            PerformanceOptimizer.getInstance(null).clearImageCache();
            
            // Request garbage collection
            System.gc();
            
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    private void performDeepCleanup() {
        try {
            // Clear all WebView data
            WebStorage.getInstance().deleteAllData();
            
            // Clear WebView database
            WebViewDatabase.getInstance(null).clearFormData();
            WebViewDatabase.getInstance(null).clearHttpAuthUsernamePassword();
            
            // Clear performance optimizer caches
            PerformanceOptimizer.getInstance(null).clearImageCache();
            PerformanceOptimizer.getInstance(null).clearWebViewCache();
            
            // Force garbage collection
            System.gc();
            
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}
