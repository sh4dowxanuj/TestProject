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
            
            // Format file size
            String sizeText = formatFileSize(download.getFileSize());
            if (download.getStatus() == DownloadItem.STATUS_DOWNLOADING && download.getFileSize() > 0) {
                sizeText = formatFileSize(download.getDownloadedSize()) + " / " + sizeText;
            }
            sizeTextView.setText(sizeText);

            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            timestampTextView.setText(sdf.format(new Date(download.getTimestamp())));

            // Set progress and status
            if (download.getStatus() == DownloadItem.STATUS_DOWNLOADING) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(download.getProgress());
                statusIcon.setImageResource(R.drawable.ic_download);
            } else {
                progressBar.setVisibility(View.GONE);
                switch (download.getStatus()) {
                    case DownloadItem.STATUS_COMPLETED:
                        statusIcon.setImageResource(android.R.drawable.stat_sys_download_done);
                        break;
                    case DownloadItem.STATUS_FAILED:
                        statusIcon.setImageResource(android.R.drawable.stat_notify_error);
                        break;
                    case DownloadItem.STATUS_PAUSED:
                        statusIcon.setImageResource(android.R.drawable.ic_media_pause);
                        break;
                    default:
                        statusIcon.setImageResource(R.drawable.ic_download);
                        break;
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
    }
}
