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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.engapp.R;
import com.example.engapp.adapters.VocabularyListAdapter;
import com.example.engapp.models.Word;
import com.example.engapp.models.WordList;
import com.example.engapp.repositories.FavoriteRepository;
import com.example.engapp.repositories.SettingsRepository;
import com.example.engapp.repositories.VocabularyRepository;
import com.example.engapp.utils.LocaleHelper;
import com.example.engapp.utils.TtsManager;

import java.util.ArrayList;
import java.util.List;

public class VocabularyListActivity extends AppCompatActivity {

    private ImageView imgBack;
    private ImageView imgGameReview;
    private TextView txtTopicTitle;
    private TextView txtTopicSubTitle;
    private RecyclerView recyclerWords;

    private VocabularyRepository vocabularyRepository;
    private FavoriteRepository favoriteRepository;
    private VocabularyListAdapter adapter;
    private TtsManager ttsManager;

    private String topicId;
    private String topicName;
    private List<Word> wordList;

    private SettingsRepository settingsRepository;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocabulary_list);

        getIntentData();
        applyVocabularyInsets();
        initViews();
        initRepositories();

        setupHeader();
        setupRecyclerView();
        setupEvents();
    }

    private void getIntentData() {
        topicId = getIntent().getStringExtra("topicId");
        topicName = getIntent().getStringExtra("topicName");

        if (topicId == null) {
            topicId = "";
        }

        if (topicName == null) {
            topicName = getString(R.string.vocabulary);
        }
    }

    private void applyVocabularyInsets() {
        View root = findViewById(R.id.layoutVocabularyRoot);
        View header = findViewById(R.id.layoutVocabularyHeader);
        View recycler = findViewById(R.id.recyclerWords);

        if (root == null || header == null || recycler == null) {
            return;
        }

        int headerLeft = header.getPaddingLeft();
        int headerTop = header.getPaddingTop();
        int headerRight = header.getPaddingRight();
        int headerBottom = header.getPaddingBottom();

        int recyclerLeft = recycler.getPaddingLeft();
        int recyclerTop = recycler.getPaddingTop();
        int recyclerRight = recycler.getPaddingRight();
        int recyclerBottom = recycler.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            header.setPadding(
                    headerLeft,
                    statusBarHeight + headerTop,
                    headerRight,
                    headerBottom
            );

            recycler.setPadding(
                    recyclerLeft,
                    recyclerTop,
                    recyclerRight,
                    navigationBarHeight + recyclerBottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void initViews() {
        imgBack = findViewById(R.id.imgBack);
        imgGameReview = findViewById(R.id.imgGameReview);
        txtTopicTitle = findViewById(R.id.txtTopicTitle);
        txtTopicSubTitle = findViewById(R.id.txtTopicSubTitle);
        recyclerWords = findViewById(R.id.recyclerWords);
    }

    private void initRepositories() {
        vocabularyRepository = new VocabularyRepository(this);
        favoriteRepository = new FavoriteRepository(this);
        ttsManager = new TtsManager(this);
        settingsRepository = new SettingsRepository(this);
    }

    private void setupHeader() {
        txtTopicTitle.setText(topicName);
    }

    // Thiết lập RecyclerView để hiển thị danh sách từ vựng
    private void setupRecyclerView() {
        wordList = vocabularyRepository.getWordsByTopicId(topicId);

        if (wordList == null) {
            wordList = new ArrayList<>();
        }

        txtTopicSubTitle.setText(getString(R.string.word_count_format, wordList.size()));

        adapter = new VocabularyListAdapter(wordList, new VocabularyListAdapter.OnWordClickListener() {
            @Override
            public void onWordClick(Word word, int position) {
                Intent intent = new Intent(VocabularyListActivity.this, FlashcardActivity.class);
                intent.putExtra("topicId", topicId);
                intent.putExtra("topicName", topicName);
                intent.putExtra("wordPosition", position);
                startActivity(intent);
            }

            @Override
            public void onAudioClick(Word word) {
                if (!settingsRepository.isSoundEnabled()) {
                    Toast.makeText(
                            VocabularyListActivity.this,
                            getString(R.string.sound_is_off),
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                ttsManager.speak(word.getWord());
            }

            @Override
            public void onFavoriteClick(Word word) {
                showSaveWordToListDialog(word);
            }
        });

        recyclerWords.setLayoutManager(new LinearLayoutManager(this));
        recyclerWords.setAdapter(adapter);
    }

    // Gắn sự kiện cho nút quay lại và nút mở game ôn tập
    private void setupEvents() {
        imgBack.setOnClickListener(v -> finish());
        imgGameReview.setOnClickListener(v -> showGameOptionsDialog());
    }

    // Hiển thị hộp thoại chọn danh sách để lưu hoặc bỏ lưu từ vựng
    private void showSaveWordToListDialog(Word word) {
        if (word == null || word.getWordId() == null) {
            return;
        }

        List<WordList> lists = favoriteRepository.getAllWordLists();

        if (lists.isEmpty()) {
            WordList defaultList = favoriteRepository.createWordList(getString(R.string.default_review_list));
            favoriteRepository.addWordToList(defaultList.getListId(), word.getWordId());

            Toast.makeText(
                    this,
                    getString(R.string.saved_to_format, getString(R.string.default_review_list)),
                    Toast.LENGTH_SHORT
            ).show();

            adapter.notifyDataSetChanged();
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

            boolean isSaved = favoriteRepository.isWordInList(list.getListId(), word.getWordId());

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

                adapter.notifyDataSetChanged();
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

    private TextView createWordListRow(String listName, boolean isSaved) {
        TextView row = new TextView(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.define_dimen_50)
        );

        params.setMargins(
                0,
                0,
                0,
                getResources().getDimensionPixelSize(R.dimen.define_dimen_8)
        );

        row.setLayoutParams(params);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_login_input_card);
        row.setPadding(
                getResources().getDimensionPixelSize(R.dimen.define_dimen_16),
                0,
                getResources().getDimensionPixelSize(R.dimen.define_dimen_16),
                0
        );

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

    private TextView createCreateNewListRow() {
        TextView row = new TextView(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.define_dimen_50)
        );

        params.setMargins(
                0,
                getResources().getDimensionPixelSize(R.dimen.define_dimen_4),
                0,
                0
        );

        row.setLayoutParams(params);
        row.setGravity(Gravity.CENTER);
        row.setBackgroundResource(R.drawable.bg_outline_button);
        row.setText(getString(R.string.create_new_list_with_plus));
        row.setTextColor(getResources().getColor(R.color.primary_blue));
        row.setTextSize(14);
        row.setTypeface(null, Typeface.BOLD);

        return row;
    }

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

            adapter.notifyDataSetChanged();
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
        intent.putExtra("topicId", topicId);
        intent.putExtra("topicName", topicName);
        intent.putExtra("gameType", gameType);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        if (ttsManager != null) {
            ttsManager.shutdown();
        }

        super.onDestroy();
    }
}