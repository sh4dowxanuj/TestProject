package com.example.testproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.testproject.adapters.BookmarkAdapter;
import com.example.testproject.database.DatabaseHelper;
import com.example.testproject.models.Bookmark;

import java.util.List;

public class BookmarksActivity extends AppCompatActivity implements BookmarkAdapter.OnBookmarkClickListener {
    private RecyclerView recyclerView;
    private BookmarkAdapter adapter;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        setupActionBar();
        initializeViews();
        loadBookmarks();
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Bookmarks");
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        databaseHelper = new DatabaseHelper(this);
    }

    private void loadBookmarks() {
        List<Bookmark> bookmarks = databaseHelper.getAllBookmarks();
        adapter = new BookmarkAdapter(bookmarks, this);
        recyclerView.setAdapter(adapter);
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
