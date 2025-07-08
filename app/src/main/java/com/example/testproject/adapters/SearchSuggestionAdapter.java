package com.example.testproject.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testproject.R;
import com.example.testproject.models.SearchSuggestion;

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
        this.suggestions = newSuggestions;
        notifyDataSetChanged();
    }

    class SuggestionViewHolder extends RecyclerView.ViewHolder {
        private TextView queryText;
        private ImageView iconView;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            queryText = itemView.findViewById(R.id.suggestionText);
            iconView = itemView.findViewById(R.id.suggestionIcon);
            
            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onSuggestionClick(suggestions.get(getAdapterPosition()));
                }
            });
        }

        public void bind(SearchSuggestion suggestion) {
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
