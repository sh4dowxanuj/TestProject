package com.example.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.view.ContextThemeWrapper;

import com.example.app.adapters.HistoryAdapter;
import com.example.app.database.DatabaseHelper;
import com.example.app.models.HistoryItem;
import com.example.app.models.Bookmark;

import java.util.List;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnHistoryClickListener {
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private HistoryAdapter adapter;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        setupSystemBars();
        setupToolbar();
        initializeViews();
        loadHistory();
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
        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        
        // Optimize RecyclerView performance
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        
        databaseHelper = new DatabaseHelper(this);
    }

    private void loadHistory() {
        List<HistoryItem> historyItems = databaseHelper.getAllHistory();
        
        if (historyItems.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            adapter = new HistoryAdapter(historyItems, this);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_clear_history) {
            databaseHelper.clearHistory();
            loadHistory();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onHistoryClick(HistoryItem historyItem) {
        // Return to MainActivity with the URL to load
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("url", historyItem.getUrl());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onHistoryDelete(HistoryItem historyItem) {
        databaseHelper.deleteHistoryItem(historyItem.getId());
        loadHistory();
    }

    @Override
    public void onHistoryLongPress(HistoryItem historyItem, View view) {
        showHistoryContextMenu(historyItem, view);
    }

    private void showHistoryContextMenu(HistoryItem historyItem, View view) {
        Context themedContext = new ContextThemeWrapper(this, R.style.BrowserPopupMenu);
        PopupMenu popup = new PopupMenu(themedContext, view);
        popup.getMenuInflater().inflate(R.menu.history_context_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.action_open_in_new_tab) {
                openInNewTab(historyItem);
                return true;
            } else if (itemId == R.id.action_copy_link) {
                copyLinkToClipboard(historyItem);
                return true;
            } else if (itemId == R.id.action_share) {
                shareHistoryItem(historyItem);
                return true;
            } else if (itemId == R.id.action_add_to_bookmarks) {
                addToBookmarks(historyItem);
                return true;
            } else if (itemId == R.id.action_delete_history) {
                onHistoryDelete(historyItem);
                return true;
            }
            
            return false;
        });
        
        popup.show();
    }

    private void openInNewTab(HistoryItem historyItem) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("url", historyItem.getUrl());
        intent.putExtra("open_in_new_tab", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void copyLinkToClipboard(HistoryItem historyItem) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("URL", historyItem.getUrl());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void shareHistoryItem(HistoryItem historyItem) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, historyItem.getUrl());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, historyItem.getTitle());
        startActivity(Intent.createChooser(shareIntent, "Share link"));
    }

    private void addToBookmarks(HistoryItem historyItem) {
        // Check if already bookmarked
        if (databaseHelper.isBookmarked(historyItem.getUrl())) {
            Toast.makeText(this, "Already bookmarked", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Add to bookmarks
        Bookmark bookmark = new Bookmark(historyItem.getTitle(), historyItem.getUrl());
        databaseHelper.addBookmark(bookmark);
        Toast.makeText(this, "Added to bookmarks", Toast.LENGTH_SHORT).show();
    }
}
