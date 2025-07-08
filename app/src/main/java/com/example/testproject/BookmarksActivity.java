package com.example.testproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.example.testproject.adapters.BookmarkAdapter;
import com.example.testproject.database.DatabaseHelper;
import com.example.testproject.models.Bookmark;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_background));
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dark_background));
        } else {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_background));
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dark_background));
        }
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
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
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
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.getUrl()));
        startActivity(intent);
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
}
