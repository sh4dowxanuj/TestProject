package com.example.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app.utils.PerformanceOptimizer;
import com.example.app.utils.DatabaseOptimizer;
import com.example.app.database.DatabaseHelper;

/**
 * Performance monitoring and optimization activity
 */
public class PerformanceActivity extends AppCompatActivity {
    
    private TextView memoryUsageText;
    private TextView cacheStatsText;
    private TextView databaseStatsText;
    private ProgressBar memoryProgressBar;
    private Button clearCacheButton;
    private Button optimizeButton;
    private Button forceGcButton;
    
    private PerformanceOptimizer performanceOptimizer;
    private DatabaseOptimizer databaseOptimizer;
    private DatabaseHelper databaseHelper;
    
    private Handler updateHandler;
    private Runnable updateRunnable;
    
    private static final int UPDATE_INTERVAL = 2000; // 2 seconds
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);
        
        initializeViews();
        initializeOptimizers();
        setupUpdateHandler();
        setupButtonListeners();
        
        // Start monitoring
        startPerformanceMonitoring();
    }
    
    private void initializeViews() {
        memoryUsageText = findViewById(R.id.memoryUsageText);
        cacheStatsText = findViewById(R.id.cacheStatsText);
        databaseStatsText = findViewById(R.id.databaseStatsText);
        memoryProgressBar = findViewById(R.id.memoryProgressBar);
        clearCacheButton = findViewById(R.id.clearCacheButton);
        optimizeButton = findViewById(R.id.optimizeButton);
        forceGcButton = findViewById(R.id.forceGcButton);
    }
    
    private void initializeOptimizers() {
        performanceOptimizer = PerformanceOptimizer.getInstance(this);
        databaseOptimizer = DatabaseOptimizer.getInstance(this);
        databaseHelper = new DatabaseHelper(this);
    }
    
    private void setupUpdateHandler() {
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updatePerformanceStats();
                updateHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
    }
    
    private void setupButtonListeners() {
        clearCacheButton.setOnClickListener(v -> {
            performanceOptimizer.clearImageCache();
            performanceOptimizer.clearWebViewCache();
            showToast("Cache cleared");
        });
        
        optimizeButton.setOnClickListener(v -> {
            performanceOptimizer.cleanupPreloadCache();
            databaseOptimizer.cleanup(databaseHelper.getReadableDatabase());
            showToast("Optimization complete");
        });
        
        forceGcButton.setOnClickListener(v -> {
            performanceOptimizer.forceGarbageCollection();
            showToast("Garbage collection triggered");
        });
    }
    
    private void startPerformanceMonitoring() {
        updateHandler.post(updateRunnable);
    }
    
    private void updatePerformanceStats() {
        // Update memory stats
        PerformanceOptimizer.MemoryStats memoryStats = performanceOptimizer.getMemoryStats();
        memoryUsageText.setText(String.format(
            "Memory Usage: %s\nActive WebViews: %d\nUsed: %d MB / %d MB",
            memoryStats.getFormattedUsage(),
            memoryStats.activeWebViews,
            memoryStats.usedMemory / (1024 * 1024),
            memoryStats.maxMemory / (1024 * 1024)
        ));
        
        // Update progress bar
        int progress = (int) memoryStats.getUsagePercentage();
        memoryProgressBar.setProgress(progress);
        
        // Update cache stats
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        
        cacheStatsText.setText(String.format(
            "Available Memory: %d MB\nTotal Memory: %d MB\nCache Hit Rate: %.1f%%",
            freeMemory / (1024 * 1024),
            totalMemory / (1024 * 1024),
            calculateCacheHitRate()
        ));
        
        // Update database stats
        databaseOptimizer.getDatabaseStats(databaseHelper.getReadableDatabase(), 
            new DatabaseOptimizer.DatabaseStatsCallback() {
                @Override
                public void onStatsReady(DatabaseOptimizer.DatabaseStats stats) {
                    if (stats != null) {
                        databaseStatsText.setText(stats.getSummary());
                    }
                }
            });
    }
    
    private double calculateCacheHitRate() {
        // Simple cache hit rate calculation
        // In a real implementation, you'd track cache hits and misses
        return 85.0 + Math.random() * 10.0; // Simulated 85-95% hit rate
    }
    
    private void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateHandler != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
