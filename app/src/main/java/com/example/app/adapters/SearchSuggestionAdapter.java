package com.example.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.R;
import com.example.app.models.SearchSuggestion;

import java.util.ArrayList;
import java.util.List;

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.SuggestionViewHolder> {
    private List<SearchSuggestion> suggestions;
    private OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(SearchSuggestion suggestion);
    }

    public SearchSuggestionAdapter(List<SearchSuggestion> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        SearchSuggestion suggestion = suggestions.get(position);
        holder.bind(suggestion);
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public void updateSuggestions(List<SearchSuggestion> newSuggestions) {
        if (newSuggestions == null) {
            newSuggestions = new ArrayList<>();
        }
        
        final List<SearchSuggestion> oldSuggestions = new ArrayList<>(this.suggestions);
        final List<SearchSuggestion> finalNewSuggestions = newSuggestions;
        
        // Use DiffUtil for efficient updates
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldSuggestions.size();
            }

            @Override
            public int getNewListSize() {
                return finalNewSuggestions.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                SearchSuggestion oldItem = oldSuggestions.get(oldItemPosition);
                SearchSuggestion newItem = finalNewSuggestions.get(newItemPosition);
                return oldItem.getQuery().equals(newItem.getQuery()) && 
                       oldItem.getType() == newItem.getType();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                SearchSuggestion oldItem = oldSuggestions.get(oldItemPosition);
                SearchSuggestion newItem = finalNewSuggestions.get(newItemPosition);
                return oldItem.getQuery().equals(newItem.getQuery()) && 
                       oldItem.getType() == newItem.getType();
            }
        });
        
        this.suggestions = finalNewSuggestions;
        diffResult.dispatchUpdatesTo(this);
    }

    class SuggestionViewHolder extends RecyclerView.ViewHolder {
        private TextView queryText;
        private ImageView iconView;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            queryText = itemView.findViewById(R.id.suggestionText);
            iconView = itemView.findViewById(R.id.suggestionIcon);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION && position < suggestions.size()) {
                    listener.onSuggestionClick(suggestions.get(position));
                }
            });
        }

        public void bind(SearchSuggestion suggestion) {
            if (suggestion == null) return;
            
            queryText.setText(suggestion.getQuery());
            
            // Set appropriate icon based on suggestion type
            switch (suggestion.getType()) {
                case SEARCH_QUERY:
                    iconView.setImageResource(R.drawable.ic_search);
                    break;
                case URL:
                    iconView.setImageResource(R.drawable.ic_arrow_forward);
                    break;
                case HISTORY:
                    iconView.setImageResource(R.drawable.ic_history);
                    break;
                case BOOKMARK:
                    iconView.setImageResource(R.drawable.ic_bookmark_filled);
                    break;
            }
        }
    }
}
