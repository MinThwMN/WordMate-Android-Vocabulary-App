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
import com.example.engapp.models.Topic;
import com.example.engapp.repositories.ProgressRepository;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

public class TopicAdapter extends RecyclerView.Adapter<TopicAdapter.TopicViewHolder> {

    private final List<Topic> topicList;
    private final OnTopicClickListener listener;
    private ProgressRepository progressRepository;

    public interface OnTopicClickListener {
        void onTopicClick(Topic topic);
    }

    public TopicAdapter(List<Topic> topicList, OnTopicClickListener listener) {
        this.topicList = topicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        if (progressRepository == null) {
            progressRepository = new ProgressRepository(context);
        }

        View view = LayoutInflater
                .from(context)
                .inflate(R.layout.item_topic_home, parent, false);

        return new TopicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        Topic topic = topicList.get(position);

        holder.txtTopicName.setText(topic.getTopicName());

        int topicImage = getTopicImage(topic.getTopicId());
        holder.imgTopicIcon.setImageResource(topicImage);

        int progress = progressRepository.getTopicProgress(topic);
        holder.progressTopicCircle.setProgress(progress);
        holder.txtTopicProgress.setText(progress + "%");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTopicClick(topic);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (topicList == null) {
            return 0;
        }

        return topicList.size();
    }

    private int getTopicImage(String topicId) {
        if (topicId == null) {
            return R.drawable.img_topic_default;
        }

        switch (topicId) {
            case "introduceyourself":
                return R.drawable.img_topic_introduceyourself;
            case "family_friends":
                return R.drawable.img_topic_family_friends;
            case "hobbies":
                return R.drawable.img_topic_hobbies;
            case "dailyroutines":
                return R.drawable.img_topic_dailyroutines;
            case "school":
                return R.drawable.img_topic_school;
            case "jobs":
                return R.drawable.img_topic_jobs;
            case "shopping":
                return R.drawable.img_topic_shopping;
            case "food_drinks":
                return R.drawable.img_topic_food_drinks;
            case "health":
                return R.drawable.img_topic_health;
            case "weather":
                return R.drawable.img_topic_weather;
            case "travel":
                return R.drawable.img_topic_travel;
            case "transportation":
                return R.drawable.img_topic_transportation;
            case "technology":
                return R.drawable.img_topic_technology;
            case "movies_music":
                return R.drawable.img_topic_movies_music;
            case "sports":
                return R.drawable.img_topic_sports;
            case "environment":
                return R.drawable.img_topic_environment;
            case "culture":
                return R.drawable.img_topic_culture;
            case "business":
                return R.drawable.img_topic_business;
            case "animals":
                return R.drawable.img_topic_animals;
            case "markets":
                return R.drawable.img_topic_markets;
            default:
                return R.drawable.img_topic_default;
        }
    }

    static class TopicViewHolder extends RecyclerView.ViewHolder {

        ImageView imgTopicIcon;
        TextView txtTopicName;
        TextView txtTopicProgress;
        CircularProgressIndicator progressTopicCircle;

        public TopicViewHolder(@NonNull View itemView) {
            super(itemView);

            imgTopicIcon = itemView.findViewById(R.id.imgTopicIcon);
            txtTopicName = itemView.findViewById(R.id.txtTopicName);
            txtTopicProgress = itemView.findViewById(R.id.txtTopicProgress);
            progressTopicCircle = itemView.findViewById(R.id.progressTopicCircle);
        }
    }
}