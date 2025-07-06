package com.example.testproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.example.testproject.adapters.HistoryAdapter;
import com.example.testproject.database.DatabaseHelper;
import com.example.testproject.models.HistoryItem;

import java.util.List;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnHistoryClickListener {
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        setupActionBar();
        initializeViews();
        loadHistory();
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("History");
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        databaseHelper = new DatabaseHelper(this);
    }

    private void loadHistory() {
        List<HistoryItem> historyItems = databaseHelper.getAllHistory();
        adapter = new HistoryAdapter(historyItems, this);
        recyclerView.setAdapter(adapter);
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
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(historyItem.getUrl()));
        startActivity(intent);
    }

    @Override
    public void onHistoryDelete(HistoryItem historyItem) {
        databaseHelper.deleteHistoryItem(historyItem.getId());
        loadHistory();
    }
}
