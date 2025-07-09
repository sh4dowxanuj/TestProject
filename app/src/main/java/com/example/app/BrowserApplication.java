package com.example.app;

import android.app.Application;
import android.content.res.Configuration;

public class BrowserApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Optimize memory management
        registerActivityLifecycleCallbacks(new MemoryOptimizedActivityLifecycleCallbacks());
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // Clean up resources when memory is low
        System.gc(); // Suggest garbage collection
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        
        switch (level) {
            case TRIM_MEMORY_RUNNING_MODERATE:
            case TRIM_MEMORY_RUNNING_LOW:
            case TRIM_MEMORY_RUNNING_CRITICAL:
                // App is running but system is low on memory
                // Release non-essential resources
                break;
            case TRIM_MEMORY_UI_HIDDEN:
                // UI is no longer visible
                // Release UI-related resources
                break;
            case TRIM_MEMORY_BACKGROUND:
            case TRIM_MEMORY_MODERATE:
            case TRIM_MEMORY_COMPLETE:
                // App is in background and system is low on memory
                // Release as much as possible
                System.gc();
                break;
        }
    }
}
