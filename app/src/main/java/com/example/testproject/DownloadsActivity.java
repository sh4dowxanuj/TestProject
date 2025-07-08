package com.example.testproject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
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
        
        boolean isEmpty = downloads.isEmpty();
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        downloadsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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
        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            // Remove from database if file doesn't exist
            databaseHelper.deleteDownloadItem(downloadItem.getId());
            loadDownloads();
            return;
        }

        try {
            Uri uri = getFileUri(file);
            String mimeType = getMimeType(downloadItem);
            Intent intent = createOpenIntent(uri, mimeType);
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                showFileOptions(downloadItem, uri, mimeType);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open this file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Uri getFileUri(File file) {
        try {
            return FileProvider.getUriForFile(this, 
                getApplicationContext().getPackageName() + ".fileprovider", file);
        } catch (Exception e) {
            return Uri.fromFile(file);
        }
    }

    private String getMimeType(DownloadItem downloadItem) {
        String mimeType = downloadItem.getMimeType();
        return (mimeType == null || mimeType.isEmpty()) 
            ? getMimeTypeFromExtension(downloadItem.getFileName()) 
            : mimeType;
    }

    private Intent createOpenIntent(Uri uri, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    private void showFileOptions(DownloadItem downloadItem, Uri uri, String mimeType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Open with...")
                .setMessage("No default app found to open this file.")
                .setPositiveButton("Choose App", (dialog, which) -> {
                    Intent intent = createOpenIntent(uri, mimeType);
                    Intent chooserIntent = Intent.createChooser(intent, "Open with");
                    if (chooserIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(chooserIntent);
                    } else {
                        Toast.makeText(this, "No app available to open this file", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Share", (dialog, which) -> shareFile(downloadItem, uri, mimeType))
                .show();
    }

    private void shareFile(DownloadItem downloadItem, Uri uri, String mimeType) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share " + downloadItem.getTitle()));
    }

    private String getMimeTypeFromExtension(String fileName) {
        if (fileName == null) return "*/*";
        
        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot != -1) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }
        
        switch (extension) {
            case "mp4": case "avi": case "mkv": case "mov": case "wmv": case "webm":
                return "video/*";
            case "mp3": case "wav": case "flac": case "aac": case "ogg":
                return "audio/*";
            case "jpg": case "jpeg": case "png": case "gif": case "bmp": case "webp":
                return "image/*";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            case "zip":
                return "application/zip";
            case "doc": case "docx":
                return "application/msword";
            default:
                return "*/*";
        }
    }

    private void showDownloadOptions(DownloadItem downloadItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(downloadItem.getTitle());
        
        String[] options;
        if (downloadItem.getStatus() == DownloadItem.STATUS_COMPLETED) {
            options = new String[]{"Open", "Share", "Delete"};
        } else {
            options = new String[]{"Delete", "Cancel download"};
        }
        
        builder.setItems(options, (dialog, which) -> {
            switch (options[which]) {
                case "Open":
                    openFile(downloadItem);
                    break;
                case "Share":
                    shareDownloadedFile(downloadItem);
                    break;
                case "Delete":
                case "Cancel download":
                    deleteDownload(downloadItem);
                    break;
            }
        });
        
        builder.show();
    }

    private void shareDownloadedFile(DownloadItem downloadItem) {
        File file = new File(downloadItem.getFilePath());
        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = getFileUri(file);
            String mimeType = getMimeType(downloadItem);
            shareFile(downloadItem, uri, mimeType);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot share this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteDownload(DownloadItem downloadItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Download")
                .setMessage("Do you want to delete this download from the list only, or also delete the file?")
                .setPositiveButton("Delete File", (dialog, which) -> {
                    deleteDownloadAndFile(downloadItem, true);
                })
                .setNeutralButton("Remove from List", (dialog, which) -> {
                    deleteDownloadAndFile(downloadItem, false);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDownloadAndFile(DownloadItem downloadItem, boolean deleteFile) {
        if (deleteFile) {
            File file = new File(downloadItem.getFilePath());
            if (file.exists()) {
                file.delete();
            }
        }
        
        databaseHelper.deleteDownloadItem(downloadItem.getId());
        loadDownloads();
        
        String message = deleteFile ? "Download and file deleted" : "Download removed from list";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDownloads(); // Refresh downloads when returning to activity
    }
}
