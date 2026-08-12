package com.example.engapp.utils;

import android.content.Context;

import com.example.engapp.R;

public class GameScoreManager {

    private int score = 0;
    private int correctCount = 0;
    private int wrongCount = 0;

    public void addCorrectAnswer() {
        score += 1;
        correctCount++;
    }

    public void addWrongAnswer() {
        wrongCount++;
    }

    public int getScore() {
        return score;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public int getWrongCount() {
        return wrongCount;
    }

    public String getScoreText() {
        return String.valueOf(score);
    }

    public String getDetailText(Context context) {
        return context.getString(
                R.string.game_detail_score_format,
                correctCount,
                wrongCount
        );
    }
}