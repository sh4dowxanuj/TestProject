package com.example.testproject.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testproject.R;
import com.example.testproject.models.HistoryItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private List<HistoryItem> historyItems;
    private OnHistoryClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnHistoryClickListener {
        void onHistoryClick(HistoryItem historyItem);
        void onHistoryDelete(HistoryItem historyItem);
    }

    public HistoryAdapter(List<HistoryItem> historyItems, OnHistoryClickListener listener) {
        this.historyItems = historyItems;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem historyItem = historyItems.get(position);
        holder.bind(historyItem);
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, urlTextView, timeTextView;
        ImageButton deleteButton;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            urlTextView = itemView.findViewById(R.id.urlTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        void bind(HistoryItem historyItem) {
            titleTextView.setText(historyItem.getTitle());
            urlTextView.setText(historyItem.getUrl());
            timeTextView.setText(dateFormat.format(new Date(historyItem.getTimestamp())));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryClick(historyItem);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryDelete(historyItem);
                }
            });
        }
    }
}
