package com.example.testproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {
    private Switch javascriptSwitch;
    private Button clearCacheButton, clearDataButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupActionBar();
        initializeViews();
        setupEventListeners();
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
    }

    private void initializeViews() {
        javascriptSwitch = findViewById(R.id.javascriptSwitch);
        clearCacheButton = findViewById(R.id.clearCacheButton);
        clearDataButton = findViewById(R.id.clearDataButton);
    }

    private void setupEventListeners() {
        clearCacheButton.setOnClickListener(v -> clearCache());
        clearDataButton.setOnClickListener(v -> clearAllData());
    }

    private void clearCache() {
        try {
            deleteDatabase("webview.db");
            deleteDatabase("webviewCache.db");
            getCacheDir().delete();
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error clearing cache", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearAllData() {
        try {
            clearCache();
            // Clear app databases
            deleteDatabase("browser.db");
            Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error clearing data", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
