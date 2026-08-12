package com.example.engapp.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.engapp.R;
import com.example.engapp.activities.FlashcardActivity;
import com.example.engapp.models.Topic;
import com.example.engapp.repositories.GoalRepository;
import com.example.engapp.repositories.ProgressRepository;
import com.example.engapp.repositories.StudySessionRepository;
import com.example.engapp.repositories.StudyTimeRepository;
import com.example.engapp.repositories.VocabularyRepository;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

public class DashboardFragment extends Fragment {

    private LinearLayout layoutTodayProgressCard;
    private LinearLayout cardReviewWords;

    private TextView txtTodayProgressDesc;
    private TextView txtTodayPercent;
    private CircularProgressIndicator progressToday;

    private TextView txtLearnedWordsNumber;
    private TextView txtReviewWordsNumber;
    private TextView txtStreakNumber;
    private TextView txtStudyTimeNumber;

    private LinearLayout layoutWeeklyBars;

    private ImageView imgCurrentTopic;
    private TextView txtCurrentTopicName;
    private TextView txtCurrentTopicProgress;

    private VocabularyRepository vocabularyRepository;
    private ProgressRepository progressRepository;
    private StudySessionRepository studySessionRepository;
    private GoalRepository goalRepository;
    private StudyTimeRepository studyTimeRepository;

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        applyDashboardInsets(view);
        initRepositories();
        setupEvents();
        loadDashboardData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initViews(View view) {
        layoutTodayProgressCard = view.findViewById(R.id.layoutTodayProgressCard);
        cardReviewWords = view.findViewById(R.id.cardReviewWords);

        txtTodayProgressDesc = view.findViewById(R.id.txtTodayProgressDesc);
        txtTodayPercent = view.findViewById(R.id.txtTodayPercent);
        progressToday = view.findViewById(R.id.progressToday);

        txtLearnedWordsNumber = view.findViewById(R.id.txtLearnedWordsNumber);
        txtReviewWordsNumber = view.findViewById(R.id.txtReviewWordsNumber);
        txtStreakNumber = view.findViewById(R.id.txtStreakNumber);
        txtStudyTimeNumber = view.findViewById(R.id.txtStudyTimeNumber);

        layoutWeeklyBars = view.findViewById(R.id.layoutWeeklyBars);

        imgCurrentTopic = view.findViewById(R.id.imgCurrentTopic);
        txtCurrentTopicName = view.findViewById(R.id.txtCurrentTopicName);
        txtCurrentTopicProgress = view.findViewById(R.id.txtCurrentTopicProgress);
    }

    private void applyDashboardInsets(View rootView) {
        View content = rootView.findViewById(R.id.layoutDashboardContent);

        if (content == null) {
            return;
        }

        int defaultLeft = content.getPaddingLeft();
        int defaultTop = content.getPaddingTop();
        int defaultRight = content.getPaddingRight();
        int defaultBottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;

            view.setPadding(
                    defaultLeft,
                    statusBarHeight + defaultTop,
                    defaultRight,
                    defaultBottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(content);
    }

    private void initRepositories() {
        vocabularyRepository = new VocabularyRepository(requireContext());
        progressRepository = new ProgressRepository(requireContext());
        studySessionRepository = new StudySessionRepository(requireContext());
        goalRepository = new GoalRepository(requireContext());
        studyTimeRepository = new StudyTimeRepository(requireContext());
    }

    private void setupEvents() {
        layoutTodayProgressCard.setOnClickListener(v -> showEditDailyGoalDialog());

        cardReviewWords.setOnClickListener(v -> openReviewWordsFlashcard());
    }

    private void loadDashboardData() {
        int learnedCount = progressRepository.getLearnedCount();
        int todayLearnedCount = progressRepository.getTodayLearnedCount();
        int reviewWordsCount = progressRepository.getReviewCount();
        int streakCount = progressRepository.getCurrentStreak();
        int studyMinutes = studyTimeRepository.getTotalStudyMinutes();

        updateTodayProgress(todayLearnedCount);
        updateStatCards(learnedCount, reviewWordsCount, streakCount, studyMinutes);
        updateWeeklyChart();
        updateCurrentTopic();
    }

    private void openReviewWordsFlashcard() {
        int reviewCount = progressRepository.getReviewCount();

        if (reviewCount <= 0) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.no_review_words),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Intent intent = new Intent(requireContext(), FlashcardActivity.class);
        intent.putExtra("source", "review_words");
        intent.putExtra("topicName", getString(R.string.review_words));
        intent.putExtra("wordPosition", 0);
        startActivity(intent);
    }

    private void updateTodayProgress(int todayLearnedCount) {
        int dailyGoal = goalRepository.getDailyGoal();

        if (dailyGoal <= 0) {
            dailyGoal = 5;
        }

        int todayPercent = (todayLearnedCount * 100) / dailyGoal;

        if (todayPercent > 100) {
            todayPercent = 100;
        }

        txtTodayProgressDesc.setText(
                getString(R.string.today_progress_dynamic_format, todayLearnedCount, dailyGoal)
        );

        progressToday.setProgress(todayPercent);
        txtTodayPercent.setText(todayPercent + "%");
    }

    private void showEditDailyGoalDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_daily_goal);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        EditText edtDailyGoal = dialog.findViewById(R.id.edtDailyGoal);
        TextView btnCancelDailyGoal = dialog.findViewById(R.id.btnCancelDailyGoal);
        TextView btnSaveDailyGoal = dialog.findViewById(R.id.btnSaveDailyGoal);

        int currentGoal = goalRepository.getDailyGoal();
        edtDailyGoal.setText(String.valueOf(currentGoal));
        edtDailyGoal.setSelection(edtDailyGoal.getText().length());
        edtDailyGoal.setInputType(InputType.TYPE_CLASS_NUMBER);

        btnCancelDailyGoal.setOnClickListener(v -> dialog.dismiss());

        btnSaveDailyGoal.setOnClickListener(v -> {
            String goalText = edtDailyGoal.getText().toString().trim();

            if (goalText.isEmpty()) {
                edtDailyGoal.setError(getString(R.string.error_enter_goal));
                edtDailyGoal.requestFocus();
                return;
            }

            int newGoal;

            try {
                newGoal = Integer.parseInt(goalText);
            } catch (NumberFormatException e) {
                newGoal = 5;
            }

            if (newGoal <= 0) {
                newGoal = 5;
            }

            if (newGoal > 100) {
                newGoal = 100;
            }

            goalRepository.saveDailyGoal(newGoal);

            Toast.makeText(
                    requireContext(),
                    getString(R.string.daily_goal_saved_format, newGoal),
                    Toast.LENGTH_SHORT
            ).show();

            dialog.dismiss();
            loadDashboardData();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void updateStatCards(
            int learnedCount,
            int reviewWordsCount,
            int streakCount,
            int studyMinutes
    ) {
        txtLearnedWordsNumber.setText(String.valueOf(learnedCount));
        txtReviewWordsNumber.setText(String.valueOf(reviewWordsCount));
        txtStreakNumber.setText(String.valueOf(streakCount));
        txtStudyTimeNumber.setText(formatStudyTime(studyMinutes));
    }

    private String formatStudyTime(int minutes) {
        if (minutes < 60) {
            return minutes + "m";
        }

        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (remainingMinutes == 0) {
            return hours + "h";
        }

        return hours + "h " + remainingMinutes + "m";
    }

    private void updateWeeklyChart() {
        if (layoutWeeklyBars == null) {
            return;
        }

        layoutWeeklyBars.removeAllViews();

        List<Integer> weeklyCounts = progressRepository.getWeeklyLearnedCounts();

        String[] labels = {
                getString(R.string.monday_short),
                getString(R.string.tuesday_short),
                getString(R.string.wednesday_short),
                getString(R.string.thursday_short),
                getString(R.string.friday_short),
                getString(R.string.saturday_short),
                getString(R.string.sunday_short)
        };

        int maxCount = 1;

        for (int count : weeklyCounts) {
            if (count > maxCount) {
                maxCount = count;
            }
        }

        int maxHeight = getResources().getDimensionPixelSize(R.dimen.define_dimen_90);
        int minHeight = getResources().getDimensionPixelSize(R.dimen.define_dimen_10);
        int barWidth = getResources().getDimensionPixelSize(R.dimen.define_dimen_18);

        for (int i = 0; i < 7; i++) {
            int count = weeklyCounts.get(i);

            int barHeight;

            if (count == 0) {
                barHeight = minHeight;
            } else {
                barHeight = (count * maxHeight) / maxCount;
            }

            LinearLayout dayLayout = new LinearLayout(requireContext());
            dayLayout.setOrientation(LinearLayout.VERTICAL);
            dayLayout.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);

            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
            );

            dayLayout.setLayoutParams(dayParams);

            TextView countText = new TextView(requireContext());
            countText.setText(String.valueOf(count));
            countText.setTextColor(getResources().getColor(R.color.primary_blue));
            countText.setTextSize(11);
            countText.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            countParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.define_dimen_4);
            countText.setLayoutParams(countParams);

            View bar = new View(requireContext());

            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    barWidth,
                    barHeight
            );

            bar.setLayoutParams(barParams);
            bar.setBackgroundResource(R.drawable.bg_chart_bar);

            TextView label = new TextView(requireContext());
            label.setText(labels[i]);
            label.setTextColor(getResources().getColor(R.color.gray));
            label.setTextSize(12);
            label.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            labelParams.topMargin = getResources().getDimensionPixelSize(R.dimen.define_dimen_8);
            label.setLayoutParams(labelParams);

            dayLayout.addView(countText);
            dayLayout.addView(bar);
            dayLayout.addView(label);

            layoutWeeklyBars.addView(dayLayout);
        }
    }

    private void updateCurrentTopic() {
        String lastTopicId = studySessionRepository.getLastTopicId();

        if (lastTopicId == null || lastTopicId.trim().isEmpty()) {
            showNoCurrentTopic();
            return;
        }

        Topic currentTopic = vocabularyRepository.getTopicById(lastTopicId);

        if (currentTopic == null) {
            showNoCurrentTopic();
            return;
        }

        int progress = progressRepository.getTopicProgress(currentTopic);

        txtCurrentTopicName.setText(currentTopic.getTopicName());
        txtCurrentTopicProgress.setText(progress + "%");
        imgCurrentTopic.setImageResource(getTopicImage(currentTopic.getTopicId()));
    }

    private void showNoCurrentTopic() {
        txtCurrentTopicName.setText(getString(R.string.no_topic));
        txtCurrentTopicProgress.setText("0%");
        imgCurrentTopic.setImageResource(R.drawable.img_topic_default);
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
}