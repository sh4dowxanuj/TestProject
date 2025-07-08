package com.example.testproject;

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

import com.example.testproject.models.SearchEngine;
import com.example.testproject.utils.SearchEnginePreferences;

public class SettingsActivity extends AppCompatActivity {
    private SwitchCompat javascriptSwitch;
    private Button clearCacheButton, clearDataButton;
    private TextView searchEngineText;
    private SearchEnginePreferences searchEnginePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        searchEnginePrefs = new SearchEnginePreferences(this);
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
        clearCacheButton = findViewById(R.id.clearCacheButton);
        clearDataButton = findViewById(R.id.clearDataButton);
        searchEngineText = findViewById(R.id.searchEngineText);
    }

    private void setupEventListeners() {
        clearCacheButton.setOnClickListener(v -> clearCache());
        clearDataButton.setOnClickListener(v -> clearAllData());
        searchEngineText.setOnClickListener(v -> showSearchEngineDialog());
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

    private void loadSettings() {
        SearchEngine currentEngine = searchEnginePrefs.getSelectedSearchEngine();
        searchEngineText.setText(currentEngine.getName());
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
