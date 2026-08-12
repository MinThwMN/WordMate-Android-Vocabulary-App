package com.example.engapp.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.engapp.models.Topic;
import com.example.engapp.models.Word;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProgressRepository {

    private static final String PREF_NAME = "progress_pref";

    private static final String KEY_VIEWED_WORDS = "viewed_words";
    private static final String KEY_LEARNED_WORDS = "learned_words";
    private static final String KEY_MASTERED_WORDS = "mastered_words";

    private static final String KEY_STUDY_DATES = "study_dates";
    private static final String KEY_DAY_WORDS_PREFIX = "day_words_";

    private static final int SCORE_NOT_STARTED = 0;
    private static final int SCORE_VIEWED = 30;
    private static final int SCORE_LEARNED = 60;
    private static final int SCORE_MASTERED = 100;

    private final SharedPreferences sharedPreferences;
    private final SimpleDateFormat dateFormat;

    public ProgressRepository(Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    private String getUserPrefix() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getUid() != null && !user.getUid().trim().isEmpty()) {
            return "user_" + user.getUid() + "_";
        }

        return "guest_";
    }

    private String getUserKey(String key) {
        return getUserPrefix() + key;
    }

    private String getDayWordsKey(String date) {
        return getUserKey(KEY_DAY_WORDS_PREFIX + date);
    }

    public void markWordAsViewed(String wordId) {
        if (isInvalidWordId(wordId)) {
            return;
        }

        String today = getTodayDate();

        Set<String> viewedSet = getStringSet(getUserKey(KEY_VIEWED_WORDS));
        viewedSet.add(wordId);

        Set<String> studyDates = getStringSet(getUserKey(KEY_STUDY_DATES));
        studyDates.add(today);

        sharedPreferences.edit()
                .putStringSet(getUserKey(KEY_VIEWED_WORDS), viewedSet)
                .putStringSet(getUserKey(KEY_STUDY_DATES), studyDates)
                .apply();
    }

    public void markWordAsLearned(String wordId) {
        if (isInvalidWordId(wordId)) {
            return;
        }

        markWordAsViewed(wordId);

        String today = getTodayDate();

        Set<String> learnedSet = getStringSet(getUserKey(KEY_LEARNED_WORDS));
        learnedSet.add(wordId);

        Set<String> todayWords = getStringSet(getDayWordsKey(today));
        todayWords.add(wordId);

        Set<String> studyDates = getStringSet(getUserKey(KEY_STUDY_DATES));
        studyDates.add(today);

        sharedPreferences.edit()
                .putStringSet(getUserKey(KEY_LEARNED_WORDS), learnedSet)
                .putStringSet(getDayWordsKey(today), todayWords)
                .putStringSet(getUserKey(KEY_STUDY_DATES), studyDates)
                .apply();
    }

    public boolean toggleWordLearned(String wordId) {
        if (isInvalidWordId(wordId)) {
            return false;
        }

        if (isWordLearned(wordId)) {
            removeLearnedWord(wordId);
            return false;
        }

        markWordAsLearned(wordId);
        return true;
    }

    public void removeLearnedWord(String wordId) {
        if (isInvalidWordId(wordId)) {
            return;
        }

        Set<String> learnedSet = getStringSet(getUserKey(KEY_LEARNED_WORDS));
        learnedSet.remove(wordId);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(getUserKey(KEY_LEARNED_WORDS), learnedSet);

        String userDayPrefix = getUserPrefix() + KEY_DAY_WORDS_PREFIX;
        Map<String, ?> allData = sharedPreferences.getAll();

        for (String key : allData.keySet()) {
            if (key == null || !key.startsWith(userDayPrefix)) {
                continue;
            }

            Set<String> dayWords = getStringSet(key);

            if (dayWords.remove(wordId)) {
                editor.putStringSet(key, dayWords);
            }
        }

        editor.apply();
    }

    public void markWordAsMastered(String wordId) {
        if (isInvalidWordId(wordId)) {
            return;
        }

        markWordAsViewed(wordId);

        Set<String> masteredSet = getStringSet(getUserKey(KEY_MASTERED_WORDS));
        masteredSet.add(wordId);

        Set<String> studyDates = getStringSet(getUserKey(KEY_STUDY_DATES));
        studyDates.add(getTodayDate());

        sharedPreferences.edit()
                .putStringSet(getUserKey(KEY_MASTERED_WORDS), masteredSet)
                .putStringSet(getUserKey(KEY_STUDY_DATES), studyDates)
                .apply();
    }

    public boolean isWordViewed(String wordId) {
        if (isInvalidWordId(wordId)) {
            return false;
        }

        return getStringSet(getUserKey(KEY_VIEWED_WORDS)).contains(wordId);
    }

    public boolean isWordLearned(String wordId) {
        if (isInvalidWordId(wordId)) {
            return false;
        }

        return getStringSet(getUserKey(KEY_LEARNED_WORDS)).contains(wordId);
    }

    public boolean isWordMastered(String wordId) {
        if (isInvalidWordId(wordId)) {
            return false;
        }

        return getStringSet(getUserKey(KEY_MASTERED_WORDS)).contains(wordId);
    }

    public int getWordProgress(String wordId) {
        if (isInvalidWordId(wordId)) {
            return SCORE_NOT_STARTED;
        }

        if (isWordMastered(wordId)) {
            return SCORE_MASTERED;
        }

        if (isWordLearned(wordId)) {
            return SCORE_LEARNED;
        }

        if (isWordViewed(wordId)) {
            return SCORE_VIEWED;
        }

        return SCORE_NOT_STARTED;
    }

    public int getTopicProgress(Topic topic) {
        if (topic == null || topic.getWords() == null || topic.getWords().isEmpty()) {
            return 0;
        }

        int totalScore = 0;

        for (Word word : topic.getWords()) {
            if (word != null) {
                totalScore += getWordProgress(word.getWordId());
            }
        }

        int maxScore = topic.getWords().size() * SCORE_MASTERED;

        if (maxScore <= 0) {
            return 0;
        }

        return Math.round((totalScore * 100f) / maxScore);
    }

    public List<String> getViewedWordIds() {
        return new ArrayList<>(getStringSet(getUserKey(KEY_VIEWED_WORDS)));
    }

    public List<String> getLearnedWordIds() {
        return new ArrayList<>(getStringSet(getUserKey(KEY_LEARNED_WORDS)));
    }

    public List<String> getMasteredWordIds() {
        return new ArrayList<>(getStringSet(getUserKey(KEY_MASTERED_WORDS)));
    }

    public int getViewedCount() {
        return getViewedWordIds().size();
    }

    public int getLearnedCount() {
        return getLearnedWordIds().size();
    }

    public int getMasteredCount() {
        return getMasteredWordIds().size();
    }

    public int getTodayLearnedCount() {
        return getLearnedCountByDate(getTodayDate());
    }

    public int getLearnedCountByDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return 0;
        }

        return getStringSet(getDayWordsKey(date)).size();
    }

    public List<Integer> getWeeklyLearnedCounts() {
        List<Integer> weeklyCounts = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday;

        if (dayOfWeek == Calendar.SUNDAY) {
            daysFromMonday = 6;
        } else {
            daysFromMonday = dayOfWeek - Calendar.MONDAY;
        }

        calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday);

        for (int i = 0; i < 7; i++) {
            String date = dateFormat.format(calendar.getTime());
            weeklyCounts.add(getLearnedCountByDate(date));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return weeklyCounts;
    }

    public int getReviewCount() {
        return getReviewWordIds().size();
    }

    public List<String> getReviewWordIds() {
        Set<String> reviewWordIds = new HashSet<>();

        reviewWordIds.addAll(getStringSet(getUserKey(KEY_VIEWED_WORDS)));
        reviewWordIds.addAll(getStringSet(getUserKey(KEY_LEARNED_WORDS)));
        reviewWordIds.removeAll(getStringSet(getUserKey(KEY_MASTERED_WORDS)));

        return new ArrayList<>(reviewWordIds);
    }

    public int getCurrentStreak() {
        Set<String> studyDates = getStringSet(getUserKey(KEY_STUDY_DATES));

        if (studyDates.isEmpty()) {
            return 0;
        }

        int streak = 0;
        Calendar calendar = Calendar.getInstance();

        while (true) {
            String date = dateFormat.format(calendar.getTime());

            if (studyDates.contains(date)) {
                streak++;
                calendar.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }

        return streak;
    }

    public int getLearnedCountByTopic(String topicId, List<String> wordIdsInTopic) {
        if (topicId == null || wordIdsInTopic == null || wordIdsInTopic.isEmpty()) {
            return 0;
        }

        int count = 0;

        for (String wordId : wordIdsInTopic) {
            if (isWordLearned(wordId)) {
                count++;
            }
        }

        return count;
    }

    public int getEstimatedStudyMinutes() {
        return getLearnedCount() * 2;
    }

    public void clearProgress() {
        String prefix = getUserPrefix();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        Map<String, ?> allData = sharedPreferences.getAll();

        for (String key : allData.keySet()) {
            if (key != null && key.startsWith(prefix)) {
                editor.remove(key);
            }
        }

        editor.apply();
    }

    private Set<String> getStringSet(String key) {
        Set<String> savedSet = sharedPreferences.getStringSet(key, new HashSet<>());

        if (savedSet == null) {
            return new HashSet<>();
        }

        return new HashSet<>(savedSet);
    }

    private boolean isInvalidWordId(String wordId) {
        return wordId == null || wordId.trim().isEmpty();
    }

    private String getTodayDate() {
        return dateFormat.format(Calendar.getInstance().getTime());
    }
}