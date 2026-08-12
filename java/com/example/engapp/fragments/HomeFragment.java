package com.example.engapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.engapp.R;
import com.example.engapp.activities.VocabularyListActivity;
import com.example.engapp.adapters.TopicAdapter;
import com.example.engapp.models.Topic;
import com.example.engapp.repositories.ProgressRepository;
import com.example.engapp.repositories.StudySessionRepository;
import com.example.engapp.repositories.VocabularyRepository;

import java.util.ArrayList;
import java.util.List;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerTopics;
    private TextView txtStreakCount;

    private VocabularyRepository vocabularyRepository;
    private StudySessionRepository studySessionRepository;
    private ProgressRepository progressRepository;
    private TopicAdapter topicAdapter;

    private final List<Topic> topicList = new ArrayList<>();

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        applyHomeInsets(view);
        initRepositories();
        setupTopicList();
        loadTopics();
        updateStreakCount();
    }

    private void initViews(View view) {
        recyclerTopics = view.findViewById(R.id.recyclerTopics);
        txtStreakCount = view.findViewById(R.id.txtStreakCount);
    }

    private void applyHomeInsets(View rootView) {
        View root = rootView.findViewById(R.id.layoutHomeRoot);
        View recyclerTopics = rootView.findViewById(R.id.recyclerTopics);

        int defaultLeft = root.getPaddingLeft();
        int defaultTop = root.getPaddingTop();
        int defaultRight = root.getPaddingRight();
        int defaultBottom = root.getPaddingBottom();

        int recyclerDefaultBottom = recyclerTopics.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            view.setPadding(
                    defaultLeft,
                    statusBarHeight + defaultTop,
                    defaultRight,
                    defaultBottom
            );

            recyclerTopics.setPadding(
                    recyclerTopics.getPaddingLeft(),
                    recyclerTopics.getPaddingTop(),
                    recyclerTopics.getPaddingRight(),
                    navigationBarHeight + recyclerDefaultBottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void initRepositories() {
        vocabularyRepository = new VocabularyRepository(requireContext());
        studySessionRepository = new StudySessionRepository(requireContext());
        progressRepository = new ProgressRepository(requireContext());
    }

    private void setupTopicList() {
        recyclerTopics.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTopics.setHasFixedSize(true);

        topicAdapter = new TopicAdapter(topicList, topic -> {
            studySessionRepository.saveLastTopic(
                    topic.getTopicId(),
                    topic.getTopicName()
            );

            Intent intent = new Intent(requireContext(), VocabularyListActivity.class);
            intent.putExtra("topicId", topic.getTopicId());
            intent.putExtra("topicName", topic.getTopicName());
            startActivity(intent);
        });

        recyclerTopics.setAdapter(topicAdapter);
    }

    private void loadTopics() {
        topicList.clear();

        List<Topic> topics = vocabularyRepository.getAllTopics();

        if (topics == null || topics.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.no_topic_data),
                    Toast.LENGTH_SHORT
            ).show();

            topicAdapter.notifyDataSetChanged();
            return;
        }

        topicList.addAll(topics);
        topicAdapter.notifyDataSetChanged();
    }

    private void updateStreakCount() {
        int streak = progressRepository.getCurrentStreak();
        txtStreakCount.setText(String.valueOf(streak));
    }

    @Override
    public void onResume() {
        super.onResume();

        updateStreakCount();

        if (topicAdapter != null) {
            topicAdapter.notifyDataSetChanged();
        }
    }
}