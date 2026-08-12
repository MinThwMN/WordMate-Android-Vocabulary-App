package com.example.engapp.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.engapp.R;
import com.example.engapp.models.Word;
import com.example.engapp.models.WordList;
import com.example.engapp.repositories.FavoriteRepository;
import com.example.engapp.repositories.ProgressRepository;
import com.example.engapp.repositories.SavedWordRepository;
import com.example.engapp.repositories.SettingsRepository;
import com.example.engapp.repositories.StudyTimeRepository;
import com.example.engapp.repositories.VocabularyRepository;
import com.example.engapp.utils.LocaleHelper;
import com.example.engapp.utils.TtsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlashcardActivity extends AppCompatActivity {

    // Các thành phần giao diện trên màn hình flashcard
    private ImageView imgBack;
    private ImageView imgGameReview;
    private ImageView imgAudio;
    private ImageView imgShuffle;
    private ImageView imgFavorite;
    private ImageView imgLearned;

    // TextView hiển thị thông tin từ vựng
    private TextView txtFlashcardTopic;
    private TextView txtFlashcardCount;
    private TextView txtWord;
    private TextView txtIpa;
    private TextView txtMeaning;
    private TextView txtExample;

    // TextView hiển thị nút điều hướng
    private TextView btnPrevious;
    private TextView btnNext;

    private ProgressBar progressFlashcard;
    private View layoutFlashcard;

    // Các repository dùng để lấy dữ liệu từ vựng, yêu thích, tiến độ và cài đặt
    private VocabularyRepository vocabularyRepository;
    private FavoriteRepository favoriteRepository;
    private ProgressRepository progressRepository;
    private SavedWordRepository savedWordRepository;
    private SettingsRepository settingsRepository;
    private StudyTimeRepository studyTimeRepository;

    // Quản lý chức năng phát âm bằng Text-to-Speech
    private TtsManager ttsManager;

    // Nhóm trạng thái học
    private String topicId;
    private String topicName;
    private int currentPosition;

    private List<Word> wordList;
    private boolean isMeaningVisible = false;

    private String source;
    private String favoriteListId;
    private String favoriteListName;

    // Nhóm tính thời gian người dùng học
    private long studyStartTime = 0;
    private boolean isTrackingStudyTime = false;


    // Áp dụng ngôn ngữ đã lưu cho Activity trước khi giao diện được tạo
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        getIntentData();
        applyFlashcardInsets();
        initViews();
        initRepositories();

        loadWords();
        setupEvents();
        showCurrentWord();
    }

    // Lấy dữ liệu được truyền từ màn hình trước qua Intent
    private void getIntentData() {
        source = getIntent().getStringExtra("source");

        topicId = getIntent().getStringExtra("topicId");
        topicName = getIntent().getStringExtra("topicName");

        favoriteListId = getIntent().getStringExtra("listId");
        favoriteListName = getIntent().getStringExtra("listName");

        currentPosition = getIntent().getIntExtra("wordPosition", 0);

        if (source == null) source = "topic";

        if (topicId == null) topicId = "";

        if (topicName == null) topicName = getString(R.string.flashcard_title);

        if (favoriteListName != null) topicName = favoriteListName;

        if ("review_words".equals(source)) topicName = getString(R.string.review_words);
    }

    // Xử lý padding để nội dung không bị che bởi status bar và navigation bar
    private void applyFlashcardInsets() {
        View content = findViewById(R.id.layoutFlashcardContent);

        if (content == null) return;

        int defaultLeft = content.getPaddingLeft();
        int defaultTop = content.getPaddingTop();
        int defaultRight = content.getPaddingRight();
        int defaultBottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            view.setPadding(
                    defaultLeft,
                    statusBarHeight + defaultTop,
                    defaultRight,
                    navigationBarHeight + defaultBottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(content);
    }

    // Ánh xạ các thành phần giao diện từ file XML
    private void initViews() {
        imgBack = findViewById(R.id.imgBack);
        imgGameReview = findViewById(R.id.imgGameReview);
        imgAudio = findViewById(R.id.imgAudio);
        imgShuffle = findViewById(R.id.imgShuffle);
        imgFavorite = findViewById(R.id.imgFavorite);
        imgLearned = findViewById(R.id.imgLearned);

        txtFlashcardTopic = findViewById(R.id.txtFlashcardTopic);
        txtFlashcardCount = findViewById(R.id.txtFlashcardCount);
        txtWord = findViewById(R.id.txtWord);
        txtIpa = findViewById(R.id.txtIpa);
        txtMeaning = findViewById(R.id.txtMeaning);
        txtExample = findViewById(R.id.txtExample);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        progressFlashcard = findViewById(R.id.progressFlashcard);
        layoutFlashcard = findViewById(R.id.layoutFlashcard);
    }

    // Khởi tạo các repository và công cụ phát âm TTS
    private void initRepositories() {
        vocabularyRepository = new VocabularyRepository(this);
        favoriteRepository = new FavoriteRepository(this);
        progressRepository = new ProgressRepository(this);
        savedWordRepository = new SavedWordRepository(this);
        settingsRepository = new SettingsRepository(this);
        studyTimeRepository = new StudyTimeRepository(this);
        ttsManager = new TtsManager(this);
    }

    // Tải danh sách từ dựa theo nguồn học: chủ đề, yêu thích hoặc ôn tập
    private void loadWords() {
        if ("favorite_list".equals(source)) loadWordsFromFavoriteList();
        else if ("review_words".equals(source)) loadReviewWords();
        else {
            wordList = vocabularyRepository.getWordsByTopicId(topicId);

            if (wordList == null) wordList = new ArrayList<>();
        }

        if (currentPosition < 0) currentPosition = 0;

        if (!wordList.isEmpty() && currentPosition >= wordList.size()) {
            currentPosition = wordList.size() - 1;
        }
    }

    // Tải các từ vựng trong danh sách yêu thích được chọn
    private void loadWordsFromFavoriteList() {
        wordList = new ArrayList<>();

        if (favoriteListId == null) return;

        List<String> wordIds = favoriteRepository.getWordIdsByListId(favoriteListId);

        if (wordIds == null || wordIds.isEmpty()) return;

        for (String wordId : wordIds) {
            Word word = vocabularyRepository.getWordById(wordId);

            if (word == null) {
                word = savedWordRepository.getWordById(wordId);
            }

            if (word != null) {
                wordList.add(word);
            }
        }
    }

    // Tải danh sách các từ cần ôn tập dựa trên tiến độ học
    private void loadReviewWords() {
        wordList = new ArrayList<>();

        List<String> reviewWordIds = progressRepository.getReviewWordIds();

        if (reviewWordIds == null || reviewWordIds.isEmpty()) return;

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

    // Gắn sự kiện click cho các nút trên màn hình flashcard
    private void setupEvents() {
        imgBack.setOnClickListener(v -> finish());
        imgGameReview.setOnClickListener(v -> showGameOptionsDialog());
        layoutFlashcard.setOnClickListener(v -> toggleMeaning());
        btnPrevious.setOnClickListener(v -> showPreviousWord());
        btnNext.setOnClickListener(v -> showNextWord());

        imgAudio.setOnClickListener(v -> {
            Word currentWord = getCurrentWord();

            if (currentWord == null) return;

            if (!settingsRepository.isSoundEnabled()) {
                Toast.makeText(
                        this,
                        getString(R.string.sound_is_off),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            ttsManager.speak(currentWord.getWord());
        });

        imgShuffle.setOnClickListener(v -> shuffleFlashcards());

        imgFavorite.setOnClickListener(v -> {
            Word currentWord = getCurrentWord();

            if (currentWord == null) return;

            showSaveWordToListDialog(currentWord);
        });

        imgLearned.setOnClickListener(v -> {
            Word currentWord = getCurrentWord();

            if (currentWord == null) return;

            boolean isLearnedNow = progressRepository.toggleWordLearned(currentWord.getWordId());

            if (isLearnedNow) {
                Toast.makeText(
                        this,
                        getString(R.string.marked_as_learned_format, currentWord.getWord()),
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        this,
                        getString(R.string.unmarked_as_learned_format, currentWord.getWord()),
                        Toast.LENGTH_SHORT
                ).show();
            }

            updateLearnedIcon(currentWord);
        });
    }

    // Hiển thị hộp thoại chọn danh sách để lưu hoặc bỏ lưu từ vựng
    private void showSaveWordToListDialog(Word word) {
        if (word == null || word.getWordId() == null) return;

        List<WordList> lists = favoriteRepository.getAllWordLists();

        if (lists.isEmpty()) {
            WordList defaultList = favoriteRepository.createWordList(
                    getString(R.string.default_review_list)
            );

            favoriteRepository.addWordToList(defaultList.getListId(), word.getWordId());

            Toast.makeText(
                    this,
                    getString(R.string.saved_to_format, getString(R.string.default_review_list)),
                    Toast.LENGTH_SHORT
            ).show();

            updateFavoriteIcon(word);
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_save_word_to_list);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        TextView txtSaveWordSubtitle = dialog.findViewById(R.id.txtSaveWordSubtitle);
        LinearLayout layoutWordListContainer = dialog.findViewById(R.id.layoutWordListContainer);
        TextView btnCloseSaveWordDialog = dialog.findViewById(R.id.btnCloseSaveWordDialog);

        txtSaveWordSubtitle.setText(
                getString(R.string.save_word_subtitle_format, word.getWord())
        );

        layoutWordListContainer.removeAllViews();

        for (WordList list : lists) {
            if (list == null || list.getListId() == null) {
                continue;
            }

            boolean isSaved = favoriteRepository.isWordInList(
                    list.getListId(),
                    word.getWordId()
            );

            TextView row = createWordListRow(list.getListName(), isSaved);

            row.setOnClickListener(v -> {
                if (favoriteRepository.isWordInList(list.getListId(), word.getWordId())) {
                    favoriteRepository.removeWordFromList(list.getListId(), word.getWordId());

                    Toast.makeText(
                            this,
                            getString(R.string.removed_from_format, list.getListName()),
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    favoriteRepository.addWordToList(list.getListId(), word.getWordId());

                    Toast.makeText(
                            this,
                            getString(R.string.saved_to_format, list.getListName()),
                            Toast.LENGTH_SHORT
                    ).show();
                }

                updateFavoriteIcon(word);
                dialog.dismiss();
            });

            layoutWordListContainer.addView(row);
        }

        TextView createNewRow = createCreateNewListRow();

        createNewRow.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateListAndSaveWordDialog(word);
        });

        layoutWordListContainer.addView(createNewRow);

        btnCloseSaveWordDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    // Hiển thị hộp thoại tạo danh sách mới và lưu từ hiện tại vào danh sách đó
    private void showCreateListAndSaveWordDialog(Word word) {
        if (word == null || word.getWordId() == null) {
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_create_word_list);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        EditText edtListName = dialog.findViewById(R.id.edtListName);
        TextView btnCancelCreateList = dialog.findViewById(R.id.btnCancelCreateList);
        TextView btnSaveCreateList = dialog.findViewById(R.id.btnSaveCreateList);

        btnCancelCreateList.setOnClickListener(v -> dialog.dismiss());

        btnSaveCreateList.setOnClickListener(v -> {
            String listName = edtListName.getText().toString().trim();

            if (listName.isEmpty()) {
                edtListName.setError(getString(R.string.list_name_empty));
                edtListName.requestFocus();
                return;
            }

            WordList newList = favoriteRepository.createWordList(listName);
            favoriteRepository.addWordToList(newList.getListId(), word.getWordId());

            Toast.makeText(
                    this,
                    getString(R.string.saved_to_format, listName),
                    Toast.LENGTH_SHORT
            ).show();

            updateFavoriteIcon(word);
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private TextView createWordListRow(String listName, boolean isSaved) {
        TextView row = new TextView(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.define_dimen_50)
        );

        params.setMargins(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.define_dimen_8));

        row.setLayoutParams(params);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_login_input_card);
        row.setPadding(getResources().getDimensionPixelSize(R.dimen.define_dimen_16), 0, getResources().getDimensionPixelSize(R.dimen.define_dimen_16), 0);

        if (isSaved) {
            row.setText("✓ " + listName);
            row.setTextColor(getResources().getColor(R.color.primary_blue));
        } else {
            row.setText(listName);
            row.setTextColor(getResources().getColor(R.color.dark_navy));
        }

        row.setTextSize(14);
        row.setTypeface(null, Typeface.BOLD);
        row.setSingleLine(true);

        return row;
    }

    // Tạo danh sách yêu thích mới
    private TextView createCreateNewListRow() {
        TextView row = new TextView(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.define_dimen_50)
        );

        params.setMargins(0, getResources().getDimensionPixelSize(R.dimen.define_dimen_4), 0, 0);

        row.setLayoutParams(params);
        row.setGravity(Gravity.CENTER);
        row.setBackgroundResource(R.drawable.bg_outline_button);
        row.setText(getString(R.string.create_new_list_with_plus));
        row.setTextColor(getResources().getColor(R.color.primary_blue));
        row.setTextSize(14);
        row.setTypeface(null, Typeface.BOLD);

        return row;
    }

    private void showCurrentWord() {
        if (wordList == null || wordList.isEmpty()) {
            txtFlashcardTopic.setText(topicName);
            txtFlashcardCount.setText(getString(R.string.empty_flashcard_count));
            txtWord.setText(getString(R.string.no_words));
            txtIpa.setText("");
            txtMeaning.setText("");
            txtExample.setText("");
            progressFlashcard.setProgress(0);
            return;
        }

        Word word = wordList.get(currentPosition);

        progressRepository.markWordAsViewed(word.getWordId());

        txtFlashcardTopic.setText(topicName);
        txtFlashcardCount.setText(
                getString(
                        R.string.flashcard_count_format,
                        currentPosition + 1,
                        wordList.size()
                )
        );

        txtWord.setText(word.getWord());
        txtIpa.setText(word.getIpa());
        String languageCode = LocaleHelper.getSavedLanguage(this);
        txtMeaning.setText(word.getMeaningByLanguage(languageCode));
        txtExample.setText(word.getExample());

        hideMeaning();

        int progress = ((currentPosition + 1) * 100) / wordList.size();
        progressFlashcard.setProgress(progress);

        updateLearnedIcon(word);
        updateButtonState();
    }

    // Ẩn hoặc hiện phần nghĩa và ví dụ khi người dùng bấm vào thẻ
    private void toggleMeaning() {
        if (wordList == null || wordList.isEmpty()) {
            return;
        }

        if (isMeaningVisible) {
            hideMeaning();
        } else {
            showMeaning();
        }
    }

    private void showMeaning() {
        isMeaningVisible = true;
        txtMeaning.setVisibility(View.VISIBLE);
        txtExample.setVisibility(View.VISIBLE);
    }

    private void hideMeaning() {
        isMeaningVisible = false;
        txtMeaning.setVisibility(View.GONE);
        txtExample.setVisibility(View.GONE);
    }

    private void showPreviousWord() {
        if (wordList == null || wordList.isEmpty()) {
            return;
        }

        if (currentPosition > 0) {
            currentPosition--;
            showCurrentWord();
        }
    }

    private void showNextWord() {
        if (wordList == null || wordList.isEmpty()) {
            return;
        }

        if (currentPosition < wordList.size() - 1) {
            currentPosition++;
            showCurrentWord();
        } else {
            Toast.makeText(
                    this,
                    getString(R.string.finished_topic),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void shuffleFlashcards() {
        if (wordList == null || wordList.size() <= 1) {
            Toast.makeText(
                    this,
                    getString(R.string.not_enough_words_to_shuffle),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String oldWordId = null;

        Word oldWord = getCurrentWord();

        if (oldWord != null) {
            oldWordId = oldWord.getWordId();
        }

        Collections.shuffle(wordList);

        if (oldWordId != null
                && wordList.get(0).getWordId() != null
                && wordList.get(0).getWordId().equals(oldWordId)
                && wordList.size() > 1) {
            Collections.swap(wordList, 0, 1);
        }

        currentPosition = 0;

        hideMeaning();
        showCurrentWord();

        Toast.makeText(
                this,
                getString(R.string.flashcards_shuffled),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void showGameOptionsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_game_options);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        View layoutGameQuiz = dialog.findViewById(R.id.layoutGameQuiz);
        View layoutGameFill = dialog.findViewById(R.id.layoutGameFill);
        View layoutGameMatch = dialog.findViewById(R.id.layoutGameMatch);
        TextView btnCloseGamePopup = dialog.findViewById(R.id.btnCloseGamePopup);

        layoutGameQuiz.setOnClickListener(v -> {
            dialog.dismiss();
            openGameActivity(0);
        });

        layoutGameFill.setOnClickListener(v -> {
            dialog.dismiss();
            openGameActivity(1);
        });

        layoutGameMatch.setOnClickListener(v -> {
            dialog.dismiss();
            openGameActivity(2);
        });

        btnCloseGamePopup.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void openGameActivity(int gameType) {
        Intent intent = new Intent(this, GameActivity.class);

        intent.putExtra("source", source);
        intent.putExtra("topicId", topicId);
        intent.putExtra("topicName", topicName);
        intent.putExtra("gameType", gameType);

        if ("favorite_list".equals(source)) {
            intent.putExtra("listId", favoriteListId);
            intent.putExtra("listName", favoriteListName);
        }

        startActivity(intent);
    }

    private Word getCurrentWord() {
        if (wordList == null || wordList.isEmpty()) {
            return null;
        }

        if (currentPosition < 0 || currentPosition >= wordList.size()) {
            return null;
        }

        return wordList.get(currentPosition);
    }

    private void updateLearnedIcon(Word word) {
        if (word == null || word.getWordId() == null) {
            imgLearned.setColorFilter(getResources().getColor(R.color.gray));
            imgFavorite.setColorFilter(getResources().getColor(R.color.gray));
            return;
        }

        if (progressRepository.isWordMastered(word.getWordId())
                || progressRepository.isWordLearned(word.getWordId())) {
            imgLearned.setColorFilter(getResources().getColor(R.color.success_green));
        } else {
            imgLearned.setColorFilter(getResources().getColor(R.color.gray));
        }

        updateFavoriteIcon(word);
    }

    private void updateFavoriteIcon(Word word) {
        if (word == null || word.getWordId() == null) {
            imgFavorite.setColorFilter(getResources().getColor(R.color.gray));
            return;
        }

        if (favoriteRepository.isWordSavedInAnyList(word.getWordId())) {
            imgFavorite.setColorFilter(getResources().getColor(R.color.error_red));
        } else {
            imgFavorite.setColorFilter(getResources().getColor(R.color.gray));
        }
    }

    // Cập nhật trạng thái nút Previous và nội dung nút Next/Finish
    private void updateButtonState() {
        if (currentPosition == 0) {
            btnPrevious.setAlpha(0.5f);
        } else {
            btnPrevious.setAlpha(1f);
        }

        if (wordList != null && currentPosition == wordList.size() - 1) {
            btnNext.setText(getString(R.string.finish));
        } else {
            btnNext.setText(getString(R.string.next));
        }
    }

    // Bắt đầu tính thời gian học khi người dùng vào màn hình flashcard
    private void startStudyTimer() {
        if (!isTrackingStudyTime) {
            studyStartTime = System.currentTimeMillis();
            isTrackingStudyTime = true;
        }
    }

    // Dừng tính thời gian học và lưu số giây học được vào repository
    private void stopStudyTimer() {
        if (isTrackingStudyTime) {
            long elapsedMillis = System.currentTimeMillis() - studyStartTime;
            long elapsedSeconds = elapsedMillis / 1000;

            studyTimeRepository.addStudyTime(elapsedSeconds);

            isTrackingStudyTime = false;
            studyStartTime = 0;
        }
    }

    // Khi Activity hiển thị lại, bắt đầu hoặc tiếp tục tính thời gian học
    @Override
    protected void onResume() {
        super.onResume();
        startStudyTimer();
    }

    // Khi người dùng rời màn hình, dừng và lưu thời gian học
    @Override
    protected void onPause() {
        stopStudyTimer();
        super.onPause();
    }

    // Giải phóng tài nguyên Text-to-Speech khi Activity bị hủy
    @Override
    protected void onDestroy() {
        if (ttsManager != null) {
            ttsManager.shutdown();
        }

        super.onDestroy();
    }
}