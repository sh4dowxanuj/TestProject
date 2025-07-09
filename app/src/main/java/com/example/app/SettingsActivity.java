package com.example.app;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.app.models.SearchEngine;
import com.example.app.utils.SearchEnginePreferences;
import com.example.app.utils.AdBlocker;

public class SettingsActivity extends AppCompatActivity {
    private SwitchCompat javascriptSwitch, adBlockerSwitch, stealthModeSwitch;
    private Button clearCacheButton, clearDataButton;
    private TextView searchEngineText, adBlockerStatusText;
    private SearchEnginePreferences searchEnginePrefs;
    private AdBlocker adBlocker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        searchEnginePrefs = new SearchEnginePreferences(this);
        adBlocker = new AdBlocker(this);
        setupSystemBars();
        setupToolbar();
        initializeViews();
        setupEventListeners();
        loadSettings();
    }

    private void setupSystemBars() {
        // Set status bar and navigation bar colors for dark theme
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_background));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dark_background));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void initializeViews() {
        javascriptSwitch = findViewById(R.id.javascriptSwitch);
        adBlockerSwitch = findViewById(R.id.adBlockerSwitch);
        stealthModeSwitch = findViewById(R.id.stealthModeSwitch);
        clearCacheButton = findViewById(R.id.clearCacheButton);
        clearDataButton = findViewById(R.id.clearDataButton);
        searchEngineText = findViewById(R.id.searchEngineText);
        adBlockerStatusText = findViewById(R.id.adBlockerStatusText);
    }

    private void setupEventListeners() {
        clearCacheButton.setOnClickListener(v -> clearCache());
        clearDataButton.setOnClickListener(v -> clearAllData());
        searchEngineText.setOnClickListener(v -> showSearchEngineDialog());
        
        adBlockerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adBlocker.setAdBlockEnabled(isChecked);
            updateAdBlockerStatus();
            if (isChecked) {
                Toast.makeText(this, "Ad Blocker enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ad Blocker disabled", Toast.LENGTH_SHORT).show();
            }
        });
        
        stealthModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adBlocker.setStealthModeEnabled(isChecked);
            updateAdBlockerStatus();
            if (isChecked) {
                Toast.makeText(this, "Stealth Mode enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Stealth Mode disabled", Toast.LENGTH_SHORT).show();
            }
        });
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
            // Reset ad blocker count
            adBlocker.resetBlockedCount();
            updateAdBlockerStatus();
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

    private void loadSettings() {
        SearchEngine currentEngine = searchEnginePrefs.getSelectedSearchEngine();
        searchEngineText.setText(currentEngine.getName());
        
        // Load ad blocker settings
        adBlockerSwitch.setChecked(adBlocker.isAdBlockEnabled());
        stealthModeSwitch.setChecked(adBlocker.isStealthModeEnabled());
        updateAdBlockerStatus();
    }
    
    private void updateAdBlockerStatus() {
        try {
            if (adBlocker.isAdBlockEnabled()) {
                int blockedCount = adBlocker.getBlockedCount();
                String statusText = String.format("Active - %d ads blocked", blockedCount);
                if (adBlocker.isStealthModeEnabled()) {
                    statusText += " (Stealth Mode)";
                }
                adBlockerStatusText.setText(statusText);
            } else {
                adBlockerStatusText.setText("Disabled");
            }
        } catch (Exception e) {
            adBlockerStatusText.setText("Error retrieving ad blocker status");
        }
    }

    private void showSearchEngineDialog() {
        String[] searchEngineNames = searchEnginePrefs.getSearchEngineNames();
        SearchEngine currentEngine = searchEnginePrefs.getSelectedSearchEngine();
        int currentSelection = currentEngine.ordinal();

        new AlertDialog.Builder(this)
                .setTitle("Select Search Engine")
                .setSingleChoiceItems(searchEngineNames, currentSelection, (dialog, which) -> {
                    SearchEngine selectedEngine = SearchEngine.fromOrdinal(which);
                    searchEnginePrefs.setSelectedSearchEngine(selectedEngine);
                    searchEngineText.setText(selectedEngine.getName());
                    Toast.makeText(this, "Search engine changed to " + selectedEngine.getName(), 
                                 Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
