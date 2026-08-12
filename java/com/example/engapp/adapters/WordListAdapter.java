package com.example.engapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.engapp.R;
import com.example.engapp.models.WordList;

import java.util.List;

public class WordListAdapter extends RecyclerView.Adapter<WordListAdapter.WordListViewHolder> {

    private final List<WordList> wordLists;
    private final OnWordListClickListener listener;

    public interface OnWordListClickListener {
        void onWordListClick(WordList wordList, int position);
        void onWordListLongClick(WordList wordList, int position);
    }

    public WordListAdapter(List<WordList> wordLists, OnWordListClickListener listener) {
        this.wordLists = wordLists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WordListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_word_list, parent, false);

        return new WordListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordListViewHolder holder, int position) {
        WordList wordList = wordLists.get(position);

        holder.txtListName.setText(wordList.getListName());
        holder.txtListCount.setText(
                holder.itemView.getContext().getString(
                        R.string.word_count_format,
                        wordList.getTotalWords()
                )
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int adapterPosition = holder.getAdapterPosition();

                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onWordListClick(wordList, adapterPosition);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                int adapterPosition = holder.getAdapterPosition();

                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onWordListLongClick(wordList, adapterPosition);
                }
            }

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return wordLists == null ? 0 : wordLists.size();
    }

    static class WordListViewHolder extends RecyclerView.ViewHolder {

        TextView txtListName;
        TextView txtListCount;

        public WordListViewHolder(@NonNull View itemView) {
            super(itemView);
            txtListName = itemView.findViewById(R.id.txtListName);
            txtListCount = itemView.findViewById(R.id.txtListCount);
        }
    }
}