package com.example.testproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testproject.adapters.DownloadAdapter;
import com.example.testproject.database.DatabaseHelper;
import com.example.testproject.models.DownloadItem;

import java.io.File;
import java.util.List;

public class DownloadsActivity extends AppCompatActivity implements DownloadAdapter.OnDownloadItemClickListener {
    
    private RecyclerView downloadsRecyclerView;
    private DownloadAdapter downloadAdapter;
    private DatabaseHelper databaseHelper;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        loadDownloads();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Downloads");
        }
    }

    private void initializeViews() {
        downloadsRecyclerView = findViewById(R.id.downloadsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        databaseHelper = new DatabaseHelper(this);
    }

    private void setupRecyclerView() {
        downloadsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        downloadAdapter = new DownloadAdapter(this, this);
        downloadsRecyclerView.setAdapter(downloadAdapter);
    }

    private void loadDownloads() {
        List<DownloadItem> downloads = databaseHelper.getAllDownloads();
        downloadAdapter.setDownloads(downloads);
        
        if (downloads.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            downloadsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            downloadsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.downloads_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_clear_downloads) {
            clearAllDownloads();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void clearAllDownloads() {
        databaseHelper.clearDownloads();
        loadDownloads();
        Toast.makeText(this, "All downloads cleared", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadItemClick(DownloadItem downloadItem) {
        if (downloadItem.getStatus() == DownloadItem.STATUS_COMPLETED) {
            openFile(downloadItem);
        } else {
            Toast.makeText(this, "Download not completed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDownloadItemLongClick(DownloadItem downloadItem) {
        // Show options menu for individual download item
        showDownloadOptions(downloadItem);
    }

    private void openFile(DownloadItem downloadItem) {
        File file = new File(downloadItem.getFilePath());
        if (file.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(file);
            intent.setDataAndType(uri, downloadItem.getMimeType());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDownloadOptions(DownloadItem downloadItem) {
        // For now, just delete the download
        databaseHelper.deleteDownloadItem(downloadItem.getId());
        loadDownloads();
        Toast.makeText(this, "Download removed", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDownloads(); // Refresh downloads when returning to activity
    }
}
