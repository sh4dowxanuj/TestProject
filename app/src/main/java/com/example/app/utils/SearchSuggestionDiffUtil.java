package com.example.app.utils;

import androidx.recyclerview.widget.DiffUtil;
import com.example.app.models.SearchSuggestion;

import java.util.List;

public class SearchSuggestionDiffUtil extends DiffUtil.Callback {
    private List<SearchSuggestion> oldList;
    private List<SearchSuggestion> newList;

    public SearchSuggestionDiffUtil(List<SearchSuggestion> oldList, List<SearchSuggestion> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList != null ? oldList.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newList != null ? newList.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        SearchSuggestion oldItem = oldList.get(oldItemPosition);
        SearchSuggestion newItem = newList.get(newItemPosition);
        return oldItem.getQuery().equals(newItem.getQuery()) && 
               oldItem.getType() == newItem.getType();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        SearchSuggestion oldItem = oldList.get(oldItemPosition);
        SearchSuggestion newItem = newList.get(newItemPosition);
        return oldItem.equals(newItem);
    }
}
