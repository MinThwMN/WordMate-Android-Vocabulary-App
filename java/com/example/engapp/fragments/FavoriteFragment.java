package com.example.engapp.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.engapp.R;
import com.example.engapp.activities.FlashcardActivity;
import com.example.engapp.adapters.FavoriteWordAdapter;
import com.example.engapp.adapters.WordListAdapter;
import com.example.engapp.models.Word;
import com.example.engapp.models.WordList;
import com.example.engapp.repositories.FavoriteRepository;
import com.example.engapp.repositories.SavedWordRepository;
import com.example.engapp.repositories.VocabularyRepository;
import com.example.engapp.utils.LocaleHelper;
import com.example.engapp.utils.TtsManager;

import java.util.ArrayList;
import java.util.List;
import com.example.engapp.repositories.SettingsRepository;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FavoriteFragment extends Fragment {

    private ImageView imgFavoriteBack;

    private TextView txtFavoriteTitle;
    private TextView txtFavoriteSubtitle;
    private TextView btnCreateList;
    private TextView btnStudyListFlashcard;
    private RecyclerView recyclerWordLists;

    private FavoriteRepository favoriteRepository;
    private VocabularyRepository vocabularyRepository;
    private SavedWordRepository savedWordRepository;
    private TtsManager ttsManager;

    private WordListAdapter wordListAdapter;
    private FavoriteWordAdapter favoriteWordAdapter;

    private final List<WordList> wordLists = new ArrayList<>();
    private final List<Word> favoriteWords = new ArrayList<>();

    private boolean isViewingWords = false;
    private String currentListId = null;
    private String currentListName = null;

    private SettingsRepository settingsRepository;

    public FavoriteFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);

        initViews(view);
        applyFavoriteInsets(view);
        initRepositories();
        setupEvents();
        showWordLists();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isViewingWords && currentListId != null) {
            showWordsInList(currentListId, currentListName);
        } else {
            showWordLists();
        }
    }

    private void initViews(View view) {
        imgFavoriteBack = view.findViewById(R.id.imgFavoriteBack);

        txtFavoriteTitle = view.findViewById(R.id.txtFavoriteTitle);
        txtFavoriteSubtitle = view.findViewById(R.id.txtFavoriteSubtitle);
        btnCreateList = view.findViewById(R.id.btnCreateList);
        btnStudyListFlashcard = view.findViewById(R.id.btnStudyListFlashcard);
        recyclerWordLists = view.findViewById(R.id.recyclerWordLists);
    }

    private void applyFavoriteInsets(View rootView) {
        View content = rootView.findViewById(R.id.layoutFavoriteContent);
        View recycler = rootView.findViewById(R.id.recyclerWordLists);

        if (content == null || recycler == null) {
            return;
        }

        int contentLeft = content.getPaddingLeft();
        int contentTop = content.getPaddingTop();
        int contentRight = content.getPaddingRight();
        int contentBottom = content.getPaddingBottom();

        int recyclerLeft = recycler.getPaddingLeft();
        int recyclerTop = recycler.getPaddingTop();
        int recyclerRight = recycler.getPaddingRight();
        int recyclerBottom = recycler.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            view.setPadding(
                    contentLeft,
                    statusBarHeight + contentTop,
                    contentRight,
                    contentBottom
            );

            recycler.setPadding(
                    recyclerLeft,
                    recyclerTop,
                    recyclerRight,
                    navigationBarHeight + recyclerBottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(content);
    }

    private void initRepositories() {
        favoriteRepository = new FavoriteRepository(requireContext());
        vocabularyRepository = new VocabularyRepository(requireContext());
        savedWordRepository = new SavedWordRepository(requireContext());
        ttsManager = new TtsManager(requireContext());
        settingsRepository = new SettingsRepository(requireContext());
    }

    private void setupEvents() {
        imgFavoriteBack.setOnClickListener(v -> showWordLists());

        btnCreateList.setOnClickListener(v -> {
            if (!isViewingWords) {
                showCreateListDialog();
            }
        });

        btnStudyListFlashcard.setOnClickListener(v -> {
            if (currentListId == null || currentListName == null) {
                return;
            }

            if (favoriteWords.isEmpty()) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.list_has_no_words),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Intent intent = new Intent(requireContext(), FlashcardActivity.class);
            intent.putExtra("source", "favorite_list");
            intent.putExtra("listId", currentListId);
            intent.putExtra("listName", currentListName);
            intent.putExtra("wordPosition", 0);
            startActivity(intent);
        });
    }

    private void showWordLists() {
        isViewingWords = false;
        currentListId = null;
        currentListName = null;

        imgFavoriteBack.setVisibility(View.GONE);
        btnCreateList.setVisibility(View.VISIBLE);
        btnStudyListFlashcard.setVisibility(View.GONE);

        txtFavoriteTitle.setText(getString(R.string.my_word_list));
        txtFavoriteSubtitle.setText(getString(R.string.favorite_subtitle));
        btnCreateList.setText(getString(R.string.create_new_list_with_plus));

        wordLists.clear();
        wordLists.addAll(favoriteRepository.getAllWordLists());

        wordListAdapter = new WordListAdapter(wordLists, new WordListAdapter.OnWordListClickListener() {
            @Override
            public void onWordListClick(WordList wordList, int position) {
                showWordsInList(wordList.getListId(), wordList.getListName());
            }

            @Override
            public void onWordListLongClick(WordList wordList, int position) {
                showDeleteListDialog(wordList, position);
            }
        });

        recyclerWordLists.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerWordLists.setAdapter(wordListAdapter);
    }

    private void showDeleteListDialog(WordList wordList, int position) {
        if (wordList == null || position == RecyclerView.NO_POSITION) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_list))
                .setMessage(getString(R.string.delete_list_message_format, wordList.getListName()))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    favoriteRepository.deleteWordList(wordList.getListId());

                    wordLists.remove(position);
                    wordListAdapter.notifyItemRemoved(position);
                    wordListAdapter.notifyItemRangeChanged(position, wordLists.size());

                    Toast.makeText(
                            requireContext(),
                            getString(R.string.deleted_format, wordList.getListName()),
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showWordsInList(String listId, String listName) {
        isViewingWords = true;
        currentListId = listId;
        currentListName = listName;

        imgFavoriteBack.setVisibility(View.VISIBLE);
        btnCreateList.setVisibility(View.GONE);

        favoriteWords.clear();

        List<String> wordIds = favoriteRepository.getWordIdsByListId(listId);

        if (wordIds != null) {
            for (String wordId : wordIds) {
                Word word = getWordFromLocalOrApi(wordId);

                if (word != null) {
                    favoriteWords.add(word);
                }
            }
        }

        txtFavoriteTitle.setText(listName);
        txtFavoriteSubtitle.setText(getString(R.string.words_in_this_list_format, favoriteWords.size()));

        if (favoriteWords.isEmpty()) {
            btnStudyListFlashcard.setVisibility(View.GONE);
        } else {
            btnStudyListFlashcard.setVisibility(View.VISIBLE);
        }

        favoriteWordAdapter = new FavoriteWordAdapter(favoriteWords, new FavoriteWordAdapter.OnFavoriteWordClickListener() {
            @Override
            public void onAudioClick(Word word) {
                if (!settingsRepository.isSoundEnabled()) {
                    Toast.makeText(requireContext(), getString(R.string.sound_is_off), Toast.LENGTH_SHORT).show();
                    return;
                }

                ttsManager.speak(word.getWord());
            }

            @Override
            public void onRemoveClick(Word word, int position) {
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }

                favoriteRepository.removeWordFromList(currentListId, word.getWordId());

                favoriteWords.remove(position);
                favoriteWordAdapter.notifyItemRemoved(position);
                favoriteWordAdapter.notifyItemRangeChanged(position, favoriteWords.size());

                txtFavoriteSubtitle.setText(getString(R.string.words_in_this_list_format, favoriteWords.size()));

                if (favoriteWords.isEmpty()) {
                    btnStudyListFlashcard.setVisibility(View.GONE);
                } else {
                    btnStudyListFlashcard.setVisibility(View.VISIBLE);
                }

                Toast.makeText(
                        requireContext(),
                        getString(R.string.removed_word_format, word.getWord()),
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onWordClick(Word word, int position) {
                String languageCode = LocaleHelper.getSavedLanguage(requireContext());

                Toast.makeText(
                        requireContext(),
                        word.getWord() + " - " + word.getMeaningByLanguage(languageCode),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        recyclerWordLists.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerWordLists.setAdapter(favoriteWordAdapter);
    }

    private Word getWordFromLocalOrApi(String wordId) {
        Word word = vocabularyRepository.getWordById(wordId);

        if (word == null) {
            word = savedWordRepository.getWordById(wordId);
        }

        return word;
    }

    private void showCreateListDialog() {
        Dialog dialog = new Dialog(requireContext());
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

            favoriteRepository.createWordList(listName);

            Toast.makeText(
                    requireContext(),
                    getString(R.string.created_format, listName),
                    Toast.LENGTH_SHORT
            ).show();

            dialog.dismiss();
            showWordLists();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    @Override
    public void onDestroyView() {
        if (ttsManager != null) {
            ttsManager.shutdown();
        }

        super.onDestroyView();
    }
}