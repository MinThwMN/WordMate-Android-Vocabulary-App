package com.example.engapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.engapp.R;
import com.example.engapp.models.Word;
import com.example.engapp.repositories.FavoriteRepository;
import com.example.engapp.utils.LocaleHelper;

import java.util.List;

public class VocabularyListAdapter extends RecyclerView.Adapter<VocabularyListAdapter.WordViewHolder> {

    private final List<Word> wordList;
    private final OnWordClickListener listener;

    public interface OnWordClickListener {
        void onWordClick(Word word, int position);
        void onAudioClick(Word word);
        void onFavoriteClick(Word word);
    }

    public VocabularyListAdapter(List<Word> wordList, OnWordClickListener listener) {
        this.wordList = wordList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_word, parent, false);

        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        Word word = wordList.get(position);
        Context context = holder.itemView.getContext();

        String languageCode = LocaleHelper.getSavedLanguage(context);

        holder.txtWord.setText(word.getWord());
        holder.txtIpa.setText(word.getIpa());
        holder.txtMeaning.setText(word.getMeaningByLanguage(languageCode));

        FavoriteRepository favoriteRepository = new FavoriteRepository(context);

        boolean isSaved = favoriteRepository.isWordSavedInAnyList(word.getWordId());

        if (isSaved) {
            holder.imgWordFavorite.setColorFilter(
                    context.getResources().getColor(R.color.error_red)
            );
        } else {
            holder.imgWordFavorite.setColorFilter(
                    context.getResources().getColor(R.color.gray)
            );
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int adapterPosition = holder.getAdapterPosition();

                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onWordClick(word, adapterPosition);
                }
            }
        });

        holder.imgWordAudio.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAudioClick(word);
            }
        });

        holder.imgWordFavorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteClick(word);
            }
        });
    }

    @Override
    public int getItemCount() {
        return wordList == null ? 0 : wordList.size();
    }

    static class WordViewHolder extends RecyclerView.ViewHolder {

        TextView txtWord;
        TextView txtIpa;
        TextView txtMeaning;
        ImageView imgWordAudio;
        ImageView imgWordFavorite;

        public WordViewHolder(@NonNull View itemView) {
            super(itemView);

            txtWord = itemView.findViewById(R.id.txtWord);
            txtIpa = itemView.findViewById(R.id.txtIpa);
            txtMeaning = itemView.findViewById(R.id.txtMeaning);
            imgWordAudio = itemView.findViewById(R.id.imgWordAudio);
            imgWordFavorite = itemView.findViewById(R.id.imgWordFavorite);
        }
    }
}