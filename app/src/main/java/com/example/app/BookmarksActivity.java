package com.example.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import com.google.android.material.textfield.TextInputEditText;

import com.example.app.adapters.BookmarkAdapter;
import com.example.app.database.DatabaseHelper;
import com.example.app.models.Bookmark;

import java.util.List;

public class BookmarksActivity extends AppCompatActivity implements BookmarkAdapter.OnBookmarkClickListener {
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private BookmarkAdapter adapter;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        setupSystemBars();
        setupToolbar();
        initializeViews();
        loadBookmarks();
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

    private void loadBookmarks() {
        List<Bookmark> bookmarks = databaseHelper.getAllBookmarks();
        
        if (bookmarks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            adapter = new BookmarkAdapter(bookmarks, this);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onBookmarkClick(Bookmark bookmark) {
        // Return to MainActivity with the URL to load
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("url", bookmark.getUrl());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBookmarkDelete(Bookmark bookmark) {
        databaseHelper.deleteBookmark(bookmark.getId());
        loadBookmarks(); // Refresh the list
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBookmarkLongPress(Bookmark bookmark, View view) {
        showBookmarkContextMenu(bookmark, view);
    }

    private void showBookmarkContextMenu(Bookmark bookmark, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.bookmark_context_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.action_open_in_new_tab) {
                openInNewTab(bookmark);
                return true;
            } else if (itemId == R.id.action_copy_link) {
                copyLinkToClipboard(bookmark);
                return true;
            } else if (itemId == R.id.action_share) {
                shareBookmark(bookmark);
                return true;
            } else if (itemId == R.id.action_edit_bookmark) {
                editBookmark(bookmark);
                return true;
            } else if (itemId == R.id.action_delete_bookmark) {
                onBookmarkDelete(bookmark);
                return true;
            }
            
            return false;
        });
        
        popup.show();
    }

    private void openInNewTab(Bookmark bookmark) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("url", bookmark.getUrl());
        intent.putExtra("open_in_new_tab", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void copyLinkToClipboard(Bookmark bookmark) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("URL", bookmark.getUrl());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void shareBookmark(Bookmark bookmark) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, bookmark.getUrl());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, bookmark.getTitle());
        startActivity(Intent.createChooser(shareIntent, "Share link"));
    }

    private void editBookmark(Bookmark bookmark) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_bookmark, null);
        
        TextInputEditText editTitle = dialogView.findViewById(R.id.editTitle);
        TextInputEditText editUrl = dialogView.findViewById(R.id.editUrl);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        
        // Set current values
        editTitle.setText(bookmark.getTitle());
        editUrl.setText(bookmark.getUrl());
        
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            String newTitle = editTitle.getText().toString().trim();
            String newUrl = editUrl.getText().toString().trim();
            
            if (newTitle.isEmpty() || newUrl.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Update bookmark
            bookmark.setTitle(newTitle);
            bookmark.setUrl(newUrl);
            databaseHelper.updateBookmark(bookmark);
            
            // Refresh the list
            loadBookmarks();
            dialog.dismiss();
            Toast.makeText(this, "Bookmark updated", Toast.LENGTH_SHORT).show();
        });
        
        dialog.show();
    }
}
