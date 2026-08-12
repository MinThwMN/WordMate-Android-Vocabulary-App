package com.example.engapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.engapp.R;

import java.util.List;

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.SuggestionViewHolder> {

    private final List<String> suggestions;
    private final OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String word);
    }

    public SearchSuggestionAdapter(List<String> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false);

        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        String word = suggestions.get(position);

        holder.txtSuggestionWord.setText(word);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSuggestionClick(word);
            }
        });
    }

    @Override
    public int getItemCount() {
        return suggestions == null ? 0 : suggestions.size();
    }

    static class SuggestionViewHolder extends RecyclerView.ViewHolder {

        TextView txtSuggestionWord;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSuggestionWord = itemView.findViewById(R.id.txtSuggestionWord);
        }
    }
}