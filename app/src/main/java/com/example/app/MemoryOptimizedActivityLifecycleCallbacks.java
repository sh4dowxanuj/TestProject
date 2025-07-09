package com.example.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.webkit.WebView;

public class MemoryOptimizedActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // Activity created
    }
    
    @Override
    public void onActivityStarted(Activity activity) {
        // Activity started
    }
    
    @Override
    public void onActivityResumed(Activity activity) {
        // Activity resumed
    }
    
    @Override
    public void onActivityPaused(Activity activity) {
        // Activity paused - good time to clean up temporary resources
        System.gc();
    }
    
    @Override
    public void onActivityStopped(Activity activity) {
        // Activity stopped
    }
    
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // Activity saving instance state
    }
    
    @Override
    public void onActivityDestroyed(Activity activity) {
        // Activity destroyed - cleanup WebViews and other resources
        if (activity instanceof MainActivity) {
            // Force cleanup of WebView resources
            System.gc();
        }
    }
}
