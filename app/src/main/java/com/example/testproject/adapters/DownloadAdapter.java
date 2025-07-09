package com.example.testproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testproject.R;
import com.example.testproject.models.DownloadItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Locale;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder> {
    
    private List<DownloadItem> downloads;
    private Context context;
    private OnDownloadItemClickListener listener;

    public interface OnDownloadItemClickListener {
        void onDownloadItemClick(DownloadItem downloadItem);
        void onDownloadItemLongClick(DownloadItem downloadItem);
    }

    public DownloadAdapter(Context context, OnDownloadItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.downloads = new ArrayList<>();
    }

    public void setDownloads(List<DownloadItem> downloads) {
        this.downloads = downloads;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_download, parent, false);
        return new DownloadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        DownloadItem download = downloads.get(position);
        holder.bind(download);
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }

    class DownloadViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView urlTextView;
        private TextView statusTextView;
        private TextView sizeTextView;
        private TextView timestampTextView;
        private ProgressBar progressBar;
        private ImageView statusIcon;

        public DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            urlTextView = itemView.findViewById(R.id.urlTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            sizeTextView = itemView.findViewById(R.id.sizeTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            progressBar = itemView.findViewById(R.id.progressBar);
            statusIcon = itemView.findViewById(R.id.statusIcon);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDownloadItemClick(downloads.get(getAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onDownloadItemLongClick(downloads.get(getAdapterPosition()));
                }
                return true;
            });
        }

        public void bind(DownloadItem download) {
            titleTextView.setText(download.getTitle());
            urlTextView.setText(download.getUrl());
            statusTextView.setText(download.getStatusText());
            
            // Set status text color based on download status
            int statusColor;
            switch (download.getStatus()) {
                case DownloadItem.STATUS_COMPLETED:
                    statusColor = context.getResources().getColor(R.color.download_status_completed, null);
                    break;
                case DownloadItem.STATUS_FAILED:
                    statusColor = context.getResources().getColor(R.color.download_status_failed, null);
                    break;
                case DownloadItem.STATUS_PAUSED:
                    statusColor = context.getResources().getColor(R.color.download_status_paused, null);
                    break;
                case DownloadItem.STATUS_DOWNLOADING:
                    statusColor = context.getResources().getColor(R.color.download_status_downloading, null);
                    break;
                default:
                    statusColor = context.getResources().getColor(R.color.text_secondary_dark, null);
                    break;
            }
            statusTextView.setTextColor(statusColor);
            
            // Format file size
            String sizeText = formatFileSize(download.getFileSize());
            if (download.getStatus() == DownloadItem.STATUS_DOWNLOADING && download.getFileSize() > 0) {
                sizeText = formatFileSize(download.getDownloadedSize()) + " / " + sizeText;
            }
            sizeTextView.setText(sizeText);

            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            timestampTextView.setText(sdf.format(new Date(download.getTimestamp())));

            // Set file type icon
            statusIcon.setImageResource(getFileTypeIcon(download));
            
            // Set progress and status
            if (download.getStatus() == DownloadItem.STATUS_DOWNLOADING) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(download.getProgress());
                // Add a small overlay or change tint for downloading status
                statusIcon.setAlpha(0.6f);
            } else {
                progressBar.setVisibility(View.GONE);
                statusIcon.setAlpha(1.0f);
                
                // For media files, you could add a play button overlay here
                if (isMediaFile(download) && download.getStatus() == DownloadItem.STATUS_COMPLETED) {
                    // Media files could have a subtle play indicator
                }
            }
        }

        private String formatFileSize(long bytes) {
            if (bytes == 0) return "Unknown size";
            
            String[] units = {"B", "KB", "MB", "GB"};
            int unitIndex = 0;
            double size = bytes;
            
            while (size >= 1024 && unitIndex < units.length - 1) {
                size /= 1024;
                unitIndex++;
            }
            
            return String.format(Locale.getDefault(), "%.1f %s", size, units[unitIndex]);
        }

        private int getFileTypeIcon(DownloadItem download) {
            String mimeType = download.getMimeType();
            String fileName = download.getFileName();
            
            if (mimeType != null) {
                if (mimeType.startsWith("image/")) {
                    return R.drawable.ic_file_image;
                } else if (mimeType.startsWith("video/")) {
                    return R.drawable.ic_file_video;
                } else if (mimeType.startsWith("audio/")) {
                    return R.drawable.ic_file_audio;
                } else if (mimeType.equals("application/pdf") || 
                          mimeType.contains("document") || 
                          mimeType.contains("text")) {
                    return R.drawable.ic_file_document;
                } else if (mimeType.contains("zip") || 
                          mimeType.contains("rar") || 
                          mimeType.contains("archive")) {
                    return R.drawable.ic_file_archive;
                }
            } else if (fileName != null) {
                // Determine by file extension if MIME type is not available
                String extension = getFileExtension(fileName).toLowerCase(Locale.ROOT);
                switch (extension) {
                    case "jpg": case "jpeg": case "png": case "gif": case "bmp": case "webp":
                        return R.drawable.ic_file_image;
                    case "mp4": case "avi": case "mkv": case "mov": case "wmv": case "webm":
                        return R.drawable.ic_file_video;
                    case "mp3": case "wav": case "flac": case "aac": case "ogg":
                        return R.drawable.ic_file_audio;
                    case "pdf": case "doc": case "docx": case "txt":
                        return R.drawable.ic_file_document;
                    case "zip": case "rar": case "7z": case "tar":
                        return R.drawable.ic_file_archive;
                }
            }
            
            return R.drawable.ic_file_document; // Default icon
        }
        
        private String getFileExtension(String fileName) {
            int lastDot = fileName.lastIndexOf('.');
            return (lastDot != -1) ? fileName.substring(lastDot + 1) : "";
        }
        
        private boolean isMediaFile(DownloadItem download) {
            String mimeType = download.getMimeType();
            String fileName = download.getFileName();
            
            if (mimeType != null) {
                return mimeType.startsWith("video/") || mimeType.startsWith("audio/");
            } else if (fileName != null) {
                String extension = getFileExtension(fileName).toLowerCase(Locale.ROOT);
                return extension.matches("mp4|avi|mkv|mov|wmv|webm|mp3|wav|flac|aac|ogg");
            }
            
            return false;
        }
    }
}
