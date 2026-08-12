package com.example.engapp.activities;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.engapp.R;
import com.example.engapp.models.Word;
import com.example.engapp.repositories.FavoriteRepository;
import com.example.engapp.repositories.ProgressRepository;
import com.example.engapp.repositories.SavedWordRepository;
import com.example.engapp.repositories.StudyTimeRepository;
import com.example.engapp.repositories.VocabularyRepository;
import com.example.engapp.utils.GameScoreManager;
import com.example.engapp.utils.LocaleHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    // Hằng số xác định chọn loại game
    private static final int GAME_QUIZ = 0;
    private static final int GAME_FILL = 1;
    private static final int GAME_MATCH = 2;

    private ImageView imgBack;
    private TextView txtGameTitle;
    private TextView txtGameTopic;
    private TextView txtGameScore;
    private TextView txtGameDetailScore;
    private TextView txtQuestionLabel;
    private TextView txtQuestion;

    private ProgressBar progressGame;

    private LinearLayout layoutQuiz;
    private TextView btnAnswerA;
    private TextView btnAnswerB;
    private TextView btnAnswerC;
    private TextView btnAnswerD;

    private LinearLayout layoutFill;
    private TextView txtMaskedWord;
    private EditText edtFillAnswer;
    private TextView btnCheckFill;

    private LinearLayout layoutMatch;
    private LinearLayout layoutMatchWords;
    private LinearLayout layoutMatchMeanings;

    private TextView btnNextQuestion;

    private VocabularyRepository vocabularyRepository;

    // Quản lý điểm, số câu đúng và số câu sai
    private GameScoreManager scoreManager;

    private ProgressRepository progressRepository;
    private FavoriteRepository favoriteRepository;
    private SavedWordRepository savedWordRepository;
    private StudyTimeRepository studyTimeRepository;

    // Nguồn mở game: topic, favorite_list hoặc review_words
    private String source;

    private String topicId;
    private String topicName;
    private String favoriteListId;
    private String favoriteListName;

    private int gameType;

    // Danh sách từ dùng để tạo câu hỏi trong game
    private List<Word> wordList = new ArrayList<>();
    // Vị trí câu hỏi hiện tại trong danh sách từ
    private int currentIndex = 0;
    // Kiểm tra người dùng đã trả lời câu hiện tại chưa
    private boolean answered = false;

    private Word currentWord;

    private TextView selectedWordView;
    private Word selectedWordForMatch;
    private int matchedCount = 0;
    private int matchTotal = 0;
    private boolean matchFinished = false;

    private long studyStartTime = 0;
    // Kiểm tra có đang tính thời gian học hay không
    private boolean isTrackingStudyTime = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        getIntentData();
        initViews();
        initRepositories();

        setupHeader();
        loadWords();
        setupEvents();
        updateScore();
        setupGameMode();
    }

    // Lấy dữ liệu được truyền từ FlashcardActivity sang GameActivity
    private void getIntentData() {
        source = getIntent().getStringExtra("source");

        topicId = getIntent().getStringExtra("topicId");
        topicName = getIntent().getStringExtra("topicName");
        gameType = getIntent().getIntExtra("gameType", 0);

        favoriteListId = getIntent().getStringExtra("listId");
        favoriteListName = getIntent().getStringExtra("listName");

        if (source == null) {
            source = "topic";
        }

        if (topicId == null) {
            topicId = "";
        }

        if (topicName == null) {
            topicName = getString(R.string.game_title);
        }

        if (favoriteListName != null) {
            topicName = favoriteListName;
        }

        if ("review_words".equals(source)) {
            topicName = getString(R.string.review_words);
        }
    }

    private void initViews() {
        imgBack = findViewById(R.id.imgBack);

        txtGameTitle = findViewById(R.id.txtGameTitle);
        txtGameTopic = findViewById(R.id.txtGameTopic);
        txtGameScore = findViewById(R.id.txtGameScore);
        txtGameDetailScore = findViewById(R.id.txtGameDetailScore);
        txtQuestionLabel = findViewById(R.id.txtQuestionLabel);
        txtQuestion = findViewById(R.id.txtQuestion);

        progressGame = findViewById(R.id.progressGame);

        layoutQuiz = findViewById(R.id.layoutQuiz);
        btnAnswerA = findViewById(R.id.btnAnswerA);
        btnAnswerB = findViewById(R.id.btnAnswerB);
        btnAnswerC = findViewById(R.id.btnAnswerC);
        btnAnswerD = findViewById(R.id.btnAnswerD);

        layoutFill = findViewById(R.id.layoutFill);
        txtMaskedWord = findViewById(R.id.txtMaskedWord);
        edtFillAnswer = findViewById(R.id.edtFillAnswer);
        btnCheckFill = findViewById(R.id.btnCheckFill);

        layoutMatch = findViewById(R.id.layoutMatch);
        layoutMatchWords = findViewById(R.id.layoutMatchWords);
        layoutMatchMeanings = findViewById(R.id.layoutMatchMeanings);

        btnNextQuestion = findViewById(R.id.btnNextQuestion);
    }

    private void initRepositories() {
        vocabularyRepository = new VocabularyRepository(this);
        scoreManager = new GameScoreManager();
        progressRepository = new ProgressRepository(this);
        favoriteRepository = new FavoriteRepository(this);
        savedWordRepository = new SavedWordRepository(this);
        studyTimeRepository = new StudyTimeRepository(this);
    }

    // Cập nhật tiêu đề game và tên chủ đề/danh sách đang chơi
    private void setupHeader() {
        txtGameTopic.setText(topicName);

        if (gameType == GAME_QUIZ) {
            txtGameTitle.setText(getString(R.string.game_quiz));
        } else if (gameType == GAME_FILL) {
            txtGameTitle.setText(getString(R.string.game_fill_missing_letters));
        } else {
            txtGameTitle.setText(getString(R.string.game_match_words));
        }
    }

    private void loadWords() {
        if ("favorite_list".equals(source)) {
            loadWordsFromFavoriteList();
        } else if ("review_words".equals(source)) {
            loadReviewWords();
        } else {
            wordList = vocabularyRepository.getWordsByTopicId(topicId);

            if (wordList == null) {
                wordList = new ArrayList<>();
            }
        }

        Collections.shuffle(wordList);
    }

    private void loadWordsFromFavoriteList() {
        wordList = new ArrayList<>();

        if (favoriteListId == null || favoriteListId.trim().isEmpty()) {
            return;
        }

        List<String> wordIds = favoriteRepository.getWordIdsByListId(favoriteListId);

        if (wordIds == null || wordIds.isEmpty()) {
            return;
        }

        for (String wordId : wordIds) {
            if (wordId == null || wordId.trim().isEmpty()) {
                continue;
            }

            Word word = vocabularyRepository.getWordById(wordId);

            if (word == null) {
                word = savedWordRepository.getWordById(wordId);
            }

            if (word != null) {
                wordList.add(word);
            }
        }
    }

    private void loadReviewWords() {
        wordList = new ArrayList<>();

        List<String> reviewWordIds = progressRepository.getReviewWordIds();

        if (reviewWordIds == null || reviewWordIds.isEmpty()) {
            return;
        }

        for (String wordId : reviewWordIds) {
            if (wordId == null || wordId.trim().isEmpty()) {
                continue;
            }

            Word word = vocabularyRepository.getWordById(wordId);

            if (word == null) {
                word = savedWordRepository.getWordById(wordId);
            }

            if (word != null) {
                wordList.add(word);
            }
        }
    }

    private void setupEvents() {
        imgBack.setOnClickListener(v -> finish());

        btnAnswerA.setOnClickListener(v -> checkQuizAnswer(btnAnswerA));
        btnAnswerB.setOnClickListener(v -> checkQuizAnswer(btnAnswerB));
        btnAnswerC.setOnClickListener(v -> checkQuizAnswer(btnAnswerC));
        btnAnswerD.setOnClickListener(v -> checkQuizAnswer(btnAnswerD));

        btnCheckFill.setOnClickListener(v -> checkFillAnswer());

        btnNextQuestion.setOnClickListener(v -> nextQuestion());
    }

    // Hiển thị giao diện phù hợp với loại game được chọn
    private void setupGameMode() {
        layoutQuiz.setVisibility(View.GONE);
        layoutFill.setVisibility(View.GONE);
        layoutMatch.setVisibility(View.GONE);

        if (wordList == null || wordList.isEmpty()) {
            txtQuestionLabel.setText("");
            txtQuestion.setText(getString(R.string.no_words));
            btnNextQuestion.setVisibility(View.GONE);
            progressGame.setProgress(0);
            return;
        }

        currentIndex = 0;
        answered = false;

        if (gameType == GAME_QUIZ) {
            layoutQuiz.setVisibility(View.VISIBLE);
            btnNextQuestion.setVisibility(View.VISIBLE);
            showQuizQuestion();
        } else if (gameType == GAME_FILL) {
            layoutFill.setVisibility(View.VISIBLE);
            btnNextQuestion.setVisibility(View.VISIBLE);
            showFillQuestion();
        } else {
            layoutMatch.setVisibility(View.VISIBLE);
            btnNextQuestion.setVisibility(View.GONE);
            showMatchGame();
        }
    }

    // Tạo và hiển thị câu hỏi trắc nghiệm chọn nghĩa đúng
    private void showQuizQuestion() {
        resetAnswerButtons();

        answered = false;
        currentWord = wordList.get(currentIndex);

        txtQuestionLabel.setVisibility(View.VISIBLE);
        txtQuestion.setVisibility(View.VISIBLE);

        txtQuestionLabel.setText(getString(R.string.choose_correct_meaning));
        txtQuestion.setText(currentWord.getWord());

        List<String> answers = new ArrayList<>();
        String languageCode = LocaleHelper.getSavedLanguage(this);
        answers.add(currentWord.getMeaningByLanguage(languageCode));

        List<Word> shuffledWords = new ArrayList<>(wordList);
        Collections.shuffle(shuffledWords);

        for (Word word : shuffledWords) {
            if (word.getWordId() != null
                    && !word.getWordId().equals(currentWord.getWordId())) {
                answers.add(word.getMeaningByLanguage(languageCode));
            }

            if (answers.size() == 4) {
                break;
            }
        }

        while (answers.size() < 4) {
            answers.add(getString(R.string.no_answer));
        }

        Collections.shuffle(answers);

        btnAnswerA.setText(getString(R.string.answer_option_a_format, answers.get(0)));
        btnAnswerB.setText(getString(R.string.answer_option_b_format, answers.get(1)));
        btnAnswerC.setText(getString(R.string.answer_option_c_format, answers.get(2)));
        btnAnswerD.setText(getString(R.string.answer_option_d_format, answers.get(3)));

        updateProgress();
    }

    // Kiểm tra đáp án người dùng chọn trong game trắc nghiệm
    private void checkQuizAnswer(TextView selectedButton) {
        if (answered || currentWord == null) {
            return;
        }

        answered = true;

        String selectedText = selectedButton.getText().toString();
        String languageCode = LocaleHelper.getSavedLanguage(this);
        boolean isCorrect = selectedText.contains(currentWord.getMeaningByLanguage(languageCode));

        if (isCorrect) {
            scoreManager.addCorrectAnswer();
            progressRepository.markWordAsMastered(currentWord.getWordId());

            selectedButton.setBackgroundResource(R.drawable.bg_game_answer_correct);
        } else {
            scoreManager.addWrongAnswer();

            selectedButton.setBackgroundResource(R.drawable.bg_game_answer_wrong);
            showCorrectQuizAnswer();
        }

        updateScore();
    }

    // Hiển thị đáp án đúng khi người dùng chọn sai trong game trắc nghiệm
    private void showCorrectQuizAnswer() {
        markCorrectButton(btnAnswerA);
        markCorrectButton(btnAnswerB);
        markCorrectButton(btnAnswerC);
        markCorrectButton(btnAnswerD);
    }

    // Đổi màu nút đáp án đúng trong game trắc nghiệm
    private void markCorrectButton(TextView button) {
        if (currentWord == null) {
            return;
        }

        String languageCode = LocaleHelper.getSavedLanguage(this);

        if (button.getText().toString().contains(currentWord.getMeaningByLanguage(languageCode))) {
            button.setBackgroundResource(R.drawable.bg_game_answer_correct);
        }
    }

    // Đưa các nút đáp án về giao diện mặc định trước khi sang câu mới
    private void resetAnswerButtons() {
        btnAnswerA.setBackgroundResource(R.drawable.bg_game_answer);
        btnAnswerB.setBackgroundResource(R.drawable.bg_game_answer);
        btnAnswerC.setBackgroundResource(R.drawable.bg_game_answer);
        btnAnswerD.setBackgroundResource(R.drawable.bg_game_answer);
    }

    // Tạo và hiển thị câu hỏi điền chữ còn thiếu
    private void showFillQuestion() {
        answered = false;
        currentWord = wordList.get(currentIndex);

        txtQuestionLabel.setVisibility(View.VISIBLE);
        txtQuestion.setVisibility(View.VISIBLE);

        txtQuestionLabel.setText(getString(R.string.fill_missing_word));
        String languageCode = LocaleHelper.getSavedLanguage(this);
        txtQuestion.setText(currentWord.getMeaningByLanguage(languageCode));

        txtMaskedWord.setText(maskWord(currentWord.getWord()));

        edtFillAnswer.setText("");
        edtFillAnswer.setEnabled(true);

        btnCheckFill.setText(getString(R.string.check_answer));
        btnCheckFill.setTextColor(getResources().getColor(R.color.white));
        btnCheckFill.setBackgroundResource(R.drawable.bg_primary_button);

        updateProgress();
    }

    // Ẩn một số chữ cái trong từ để tạo gợi ý cho game điền từ
    private String maskWord(String word) {
        if (word == null || word.length() <= 2) {
            return word;
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);

            if (c == ' ') {
                builder.append("  ");
            } else if (i == 0 || i == word.length() - 1) {
                builder.append(c).append(" ");
            } else if (i % 2 == 0) {
                builder.append(c).append(" ");
            } else {
                builder.append("_ ");
            }
        }

        return builder.toString().trim();
    }

    private void checkFillAnswer() {
        if (answered || currentWord == null) {
            return;
        }

        String answer = edtFillAnswer.getText().toString().trim();
        String correct = currentWord.getWord().trim();

        answered = true;
        edtFillAnswer.setEnabled(false);

        if (answer.equalsIgnoreCase(correct)) {
            scoreManager.addCorrectAnswer();
            progressRepository.markWordAsMastered(currentWord.getWordId());

            btnCheckFill.setText(getString(R.string.correct_plus_one_short));
            btnCheckFill.setTextColor(getResources().getColor(R.color.dark_navy));
            btnCheckFill.setBackgroundResource(R.drawable.bg_game_answer_correct);
        } else {
            scoreManager.addWrongAnswer();

            btnCheckFill.setText(getString(R.string.answer_format, correct));
            btnCheckFill.setTextColor(getResources().getColor(R.color.dark_navy));
            btnCheckFill.setBackgroundResource(R.drawable.bg_game_answer_wrong);
        }

        updateScore();
    }

    private void showMatchGame() {
        txtQuestionLabel.setVisibility(View.GONE);
        txtQuestion.setVisibility(View.GONE);

        layoutMatchWords.removeAllViews();
        layoutMatchMeanings.removeAllViews();

        List<Word> matchWords = new ArrayList<>(wordList);
        Collections.shuffle(matchWords);

        if (matchWords.size() > 4) {
            matchWords = new ArrayList<>(matchWords.subList(0, 4));
        }

        List<Word> meanings = new ArrayList<>(matchWords);
        Collections.shuffle(meanings);

        matchedCount = 0;
        matchTotal = matchWords.size();
        matchFinished = false;
        selectedWordView = null;
        selectedWordForMatch = null;

        String languageCode = LocaleHelper.getSavedLanguage(this);

        for (Word word : matchWords) {
            TextView wordView = createMatchItem(word.getWord());

            wordView.setOnClickListener(v -> {
                if (matchFinished || !wordView.isEnabled()) {
                    return;
                }

                resetSelectedWordBackgrounds();

                selectedWordView = wordView;
                selectedWordForMatch = word;

                wordView.setBackgroundResource(R.drawable.bg_game_answer_selected);
            });

            layoutMatchWords.addView(wordView);
        }

        for (Word word : meanings) {
            String meaningText = word.getMeaningByLanguage(languageCode);

            if (meaningText == null || meaningText.trim().isEmpty()) {
                meaningText = getString(R.string.no_definition_found);
            }

            TextView meaningView = createMatchItem(meaningText);

            meaningView.setOnClickListener(v -> checkMatchAnswer(meaningView, word));

            layoutMatchMeanings.addView(meaningView);
        }

        updateProgress();
    }

    private TextView createMatchItem(String text) {
        TextView textView = new TextView(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.define_dimen_56)
        );

        params.setMargins(
                0,
                0,
                0,
                getResources().getDimensionPixelSize(R.dimen.define_dimen_10)
        );

        textView.setLayoutParams(params);
        textView.setGravity(Gravity.CENTER);
        textView.setText(text);
        textView.setTextColor(getResources().getColor(R.color.dark_navy));
        textView.setTextSize(14);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setBackgroundResource(R.drawable.bg_game_answer);

        textView.setPadding(
                getResources().getDimensionPixelSize(R.dimen.define_dimen_8),
                0,
                getResources().getDimensionPixelSize(R.dimen.define_dimen_8),
                0
        );

        return textView;
    }

    private void resetSelectedWordBackgrounds() {
        for (int i = 0; i < layoutMatchWords.getChildCount(); i++) {
            View child = layoutMatchWords.getChildAt(i);

            if (child.isEnabled()) {
                child.setBackgroundResource(R.drawable.bg_game_answer);
            }
        }
    }

    private void checkMatchAnswer(TextView meaningView, Word meaningWord) {
        if (matchFinished) {
            return;
        }

        if (selectedWordForMatch == null || selectedWordView == null) {
            Toast.makeText(
                    this,
                    getString(R.string.choose_word_first),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (selectedWordForMatch.getWordId().equals(meaningWord.getWordId())) {
            scoreManager.addCorrectAnswer();
            progressRepository.markWordAsMastered(selectedWordForMatch.getWordId());

            matchedCount++;

            selectedWordView.setEnabled(false);
            meaningView.setEnabled(false);

            selectedWordView.setBackgroundResource(R.drawable.bg_game_answer_correct);
            meaningView.setBackgroundResource(R.drawable.bg_game_answer_correct);

            selectedWordView.setAlpha(0.6f);
            meaningView.setAlpha(0.6f);

            selectedWordForMatch = null;
            selectedWordView = null;

            updateScore();
            updateProgress();

            if (matchedCount >= matchTotal) {
                matchFinished = true;
                progressGame.setProgress(100);

                Toast.makeText(
                        this,
                        getString(R.string.match_completed),
                        Toast.LENGTH_SHORT
                ).show();

                showResultDialog();
            }
        } else {
            scoreManager.addWrongAnswer();

            meaningView.setBackgroundResource(R.drawable.bg_game_answer_wrong);

            Toast.makeText(
                    this,
                    getString(R.string.try_again),
                    Toast.LENGTH_SHORT
            ).show();

            meaningView.postDelayed(() -> {
                if (meaningView.isEnabled()) {
                    meaningView.setBackgroundResource(R.drawable.bg_game_answer);
                }

                if (selectedWordView != null && selectedWordView.isEnabled()) {
                    selectedWordView.setBackgroundResource(R.drawable.bg_game_answer);
                }

                selectedWordForMatch = null;
                selectedWordView = null;
            }, 500);

            updateScore();
        }
    }

    private void nextQuestion() {
        if (gameType == GAME_MATCH) {
            return;
        }

        if (!answered) {
            Toast.makeText(
                    this,
                    getString(R.string.answer_before_next),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (currentIndex < wordList.size() - 1) {
            currentIndex++;

            if (gameType == GAME_QUIZ) {
                showQuizQuestion();
            } else if (gameType == GAME_FILL) {
                showFillQuestion();
            }
        } else {
            showResultDialog();
        }
    }

    private void updateScore() {
        txtGameScore.setText(scoreManager.getScoreText());
        txtGameDetailScore.setText(scoreManager.getDetailText(this));
    }

    private void updateProgress() {
        if (wordList == null || wordList.isEmpty()) {
            progressGame.setProgress(0);
            return;
        }

        int progress;

        if (gameType == GAME_MATCH) {
            if (matchTotal == 0) {
                progress = 0;
            } else {
                progress = (matchedCount * 100) / matchTotal;
            }
        } else {
            progress = ((currentIndex + 1) * 100) / wordList.size();
        }

        progressGame.setProgress(progress);
    }

    private void showResultDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_game_result);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        TextView txtResultScore = dialog.findViewById(R.id.txtResultScore);
        TextView txtResultDetail = dialog.findViewById(R.id.txtResultDetail);
        TextView btnResultReplay = dialog.findViewById(R.id.btnResultReplay);
        TextView btnResultExit = dialog.findViewById(R.id.btnResultExit);

        txtResultScore.setText(
                getString(R.string.score_point_format, scoreManager.getScore())
        );

        txtResultDetail.setText(
                getString(
                        R.string.game_detail_score_format,
                        scoreManager.getCorrectCount(),
                        scoreManager.getWrongCount()
                )
        );

        btnResultReplay.setOnClickListener(v -> {
            dialog.dismiss();
            restartGame();
        });

        btnResultExit.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void restartGame() {
        currentIndex = 0;
        matchedCount = 0;
        matchTotal = 0;
        matchFinished = false;
        answered = false;
        selectedWordView = null;
        selectedWordForMatch = null;

        scoreManager = new GameScoreManager();
        updateScore();

        loadWords();
        setupGameMode();
    }

    private void startStudyTimer() {
        if (!isTrackingStudyTime) {
            studyStartTime = System.currentTimeMillis();
            isTrackingStudyTime = true;
        }
    }

    private void stopStudyTimer() {
        if (isTrackingStudyTime) {
            long elapsedMillis = System.currentTimeMillis() - studyStartTime;
            long elapsedSeconds = elapsedMillis / 1000;

            studyTimeRepository.addStudyTime(elapsedSeconds);

            isTrackingStudyTime = false;
            studyStartTime = 0;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startStudyTimer();
    }

    @Override
    protected void onPause() {
        stopStudyTimer();
        super.onPause();
    }
}