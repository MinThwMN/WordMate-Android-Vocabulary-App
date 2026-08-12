package com.example.engapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.engapp.R;
import com.example.engapp.models.Word;
import com.example.engapp.utils.LocaleHelper;

import java.util.List;

public class FavoriteWordAdapter extends RecyclerView.Adapter<FavoriteWordAdapter.FavoriteWordViewHolder> {

    private final List<Word> wordList;
    private final OnFavoriteWordClickListener listener;

    public interface OnFavoriteWordClickListener {
        void onAudioClick(Word word);
        void onRemoveClick(Word word, int position);
        void onWordClick(Word word, int position);
    }

    public FavoriteWordAdapter(List<Word> wordList, OnFavoriteWordClickListener listener) {
        this.wordList = wordList;
        this.listener = listener;
    }

    // Tạo ViewHolder cho từng item trong RecyclerView
    @NonNull
    @Override
    public FavoriteWordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_favorite_word, parent, false);

        return new FavoriteWordViewHolder(view);
    }

    // Gán dữ liệu từ vựng vào từng item và thiết lập sự kiện click
    @Override
    public void onBindViewHolder(@NonNull FavoriteWordViewHolder holder, int position) {
        Word word = wordList.get(position);

        holder.txtFavoriteWord.setText(word.getWord());
        holder.txtFavoriteIpa.setText(word.getIpa());
        String languageCode = LocaleHelper.getSavedLanguage(holder.itemView.getContext());
        holder.txtFavoriteMeaning.setText(word.getMeaningByLanguage(languageCode));

        holder.imgFavoriteAudio.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAudioClick(word);
            }
        });

        holder.imgRemoveFavorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClick(word, holder.getAdapterPosition());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWordClick(word, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        if (wordList == null) {
            return 0;
        }

        return wordList.size();
    }

    static class FavoriteWordViewHolder extends RecyclerView.ViewHolder {

        TextView txtFavoriteWord;
        TextView txtFavoriteIpa;
        TextView txtFavoriteMeaning;
        ImageView imgFavoriteAudio;
        ImageView imgRemoveFavorite;

        public FavoriteWordViewHolder(@NonNull View itemView) {
            super(itemView);

            txtFavoriteWord = itemView.findViewById(R.id.txtFavoriteWord);
            txtFavoriteIpa = itemView.findViewById(R.id.txtFavoriteIpa);
            txtFavoriteMeaning = itemView.findViewById(R.id.txtFavoriteMeaning);
            imgFavoriteAudio = itemView.findViewById(R.id.imgFavoriteAudio);
            imgRemoveFavorite = itemView.findViewById(R.id.imgRemoveFavorite);
        }
    }
}