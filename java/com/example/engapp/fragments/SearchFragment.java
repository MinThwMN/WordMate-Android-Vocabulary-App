package com.example.engapp.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.engapp.R;
import com.example.engapp.adapters.SearchSuggestionAdapter;
import com.example.engapp.models.Word;
import com.example.engapp.models.WordList;
import com.example.engapp.network.DatamuseApiService;
import com.example.engapp.network.DatamuseRetrofitClient;
import com.example.engapp.network.DictionaryApiService;
import com.example.engapp.network.RetrofitClient;
import com.example.engapp.network.models.DatamuseSuggestion;
import com.example.engapp.network.models.DictionaryResponse;
import com.example.engapp.repositories.FavoriteRepository;
import com.example.engapp.repositories.SavedWordRepository;
import com.example.engapp.repositories.SettingsRepository;
import com.example.engapp.utils.LocaleHelper;
import com.example.engapp.utils.TtsManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private EditText edtSearchWord;
    private ImageView imgSearchIcon;
    private ProgressBar progressSearchLoading;
    private TextView txtSearchMessage;
    private LinearLayout layoutSearchResult;

    private TextView txtSearchWord;
    private TextView txtSearchIpa;
    private TextView txtSearchDefinition;
    private TextView txtSearchExample;

    private ImageView imgAudio;
    private ImageView imgSearchFavorite;

    private RecyclerView recyclerSearchSuggestions;
    private SearchSuggestionAdapter suggestionAdapter;

    private DictionaryApiService dictionaryApiService;
    private DatamuseApiService datamuseApiService;
    private FavoriteRepository favoriteRepository;
    private SavedWordRepository savedWordRepository;
    private SettingsRepository settingsRepository;
    private TtsManager ttsManager;

    private final List<String> visibleSuggestions = new ArrayList<>();

    private final Handler suggestionHandler = new Handler(Looper.getMainLooper());
    private Call<List<DatamuseSuggestion>> currentSuggestionCall;

    private Word currentApiWord;
    private boolean isSelectingSuggestion = false;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        initViews(view);
        applySearchInsets(view);
        initRepositories();
        setupSuggestions();
        setupEvents();
        showInitialState();

        return view;
    }

    private void initViews(View view) {
        edtSearchWord = view.findViewById(R.id.edtSearchWord);
        imgSearchIcon = view.findViewById(R.id.imgSearchIcon);
        progressSearchLoading = view.findViewById(R.id.progressSearchLoading);
        txtSearchMessage = view.findViewById(R.id.txtSearchMessage);
        layoutSearchResult = view.findViewById(R.id.layoutSearchResult);

        txtSearchWord = view.findViewById(R.id.txtSearchWord);
        txtSearchIpa = view.findViewById(R.id.txtSearchIpa);
        txtSearchDefinition = view.findViewById(R.id.txtSearchDefinition);
        txtSearchExample = view.findViewById(R.id.txtSearchExample);

        imgAudio = view.findViewById(R.id.imgAudio);
        imgSearchFavorite = view.findViewById(R.id.imgSearchFavorite);

        recyclerSearchSuggestions = view.findViewById(R.id.recyclerSearchSuggestions);
    }

    private void applySearchInsets(View rootView) {
        View content = rootView.findViewById(R.id.layoutSearchContent);

        if (content == null) {
            return;
        }

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

    private void initRepositories() {
        dictionaryApiService = RetrofitClient.getDictionaryApiService();
        datamuseApiService = DatamuseRetrofitClient.getDatamuseApiService();

        favoriteRepository = new FavoriteRepository(requireContext());
        savedWordRepository = new SavedWordRepository(requireContext());
        settingsRepository = new SettingsRepository(requireContext());
        ttsManager = new TtsManager(requireContext());
    }

    private void setupSuggestions() {
        suggestionAdapter = new SearchSuggestionAdapter(visibleSuggestions, word -> {
            isSelectingSuggestion = true;

            edtSearchWord.setText(word);
            edtSearchWord.setSelection(word.length());

            isSelectingSuggestion = false;

            hideSuggestions();
            searchWordFromApi(word);
        });

        recyclerSearchSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerSearchSuggestions.setAdapter(suggestionAdapter);
    }

    private void setupEvents() {
        imgSearchIcon.setOnClickListener(v -> handleSearch());

        edtSearchWord.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSelectingSuggestion) {
                    return;
                }

                requestSuggestionsWithDelay(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action
            }
        });

        edtSearchWord.setOnEditorActionListener((v, actionId, event) -> {
            boolean isKeyboardSearch = actionId == EditorInfo.IME_ACTION_SEARCH;
            boolean isEnterKey = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;

            if (isKeyboardSearch || isEnterKey) {
                handleSearch();
                return true;
            }

            return false;
        });

        imgAudio.setOnClickListener(v -> {
            if (currentApiWord == null) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.search_a_word_first),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (!settingsRepository.isSoundEnabled()) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.sound_is_off),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            ttsManager.speak(currentApiWord.getWord());
        });

        imgSearchFavorite.setOnClickListener(v -> {
            if (currentApiWord == null) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.search_a_word_first),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            showSaveWordToListDialog(currentApiWord);
        });
    }

    private void requestSuggestionsWithDelay(String query) {
        suggestionHandler.removeCallbacksAndMessages(null);

        String keyword = query.trim().toLowerCase();

        if (keyword.isEmpty()) {
            hideSuggestions();
            return;
        }

        suggestionHandler.postDelayed(() -> fetchSuggestionsFromApi(keyword), 250);
    }

    private void fetchSuggestionsFromApi(String query) {
        if (currentSuggestionCall != null) {
            currentSuggestionCall.cancel();
        }

        currentSuggestionCall = datamuseApiService.getSuggestions(query, 8);

        currentSuggestionCall.enqueue(new Callback<List<DatamuseSuggestion>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<DatamuseSuggestion>> call,
                    @NonNull Response<List<DatamuseSuggestion>> response
            ) {
                if (!isAdded()) {
                    return;
                }

                if (call.isCanceled()) {
                    return;
                }

                visibleSuggestions.clear();

                if (response.isSuccessful()
                        && response.body() != null
                        && !response.body().isEmpty()) {

                    for (DatamuseSuggestion suggestion : response.body()) {
                        if (suggestion.getWord() != null
                                && !suggestion.getWord().trim().isEmpty()) {
                            visibleSuggestions.add(suggestion.getWord().trim());
                        }
                    }
                }

                if (visibleSuggestions.isEmpty()) {
                    hideSuggestions();
                } else {
                    recyclerSearchSuggestions.setVisibility(View.VISIBLE);
                    suggestionAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<DatamuseSuggestion>> call,
                    @NonNull Throwable t
            ) {
                if (call.isCanceled()) {
                    return;
                }

                hideSuggestions();
            }
        });
    }

    private void hideSuggestions() {
        if (recyclerSearchSuggestions != null) {
            recyclerSearchSuggestions.setVisibility(View.GONE);
        }
    }

    private void handleSearch() {
        String word = edtSearchWord.getText().toString().trim();

        if (word.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.enter_word_first),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        hideSuggestions();
        searchWordFromApi(word);
    }

    private void showInitialState() {
        currentApiWord = null;
        progressSearchLoading.setVisibility(View.GONE);
        txtSearchMessage.setVisibility(View.GONE);
        layoutSearchResult.setVisibility(View.GONE);
        hideSuggestions();
    }

    private void showLoadingState() {
        currentApiWord = null;
        progressSearchLoading.setVisibility(View.VISIBLE);
        txtSearchMessage.setVisibility(View.GONE);
        layoutSearchResult.setVisibility(View.GONE);
        hideSuggestions();
    }

    private void showMessage(String message, boolean isError) {
        progressSearchLoading.setVisibility(View.GONE);
        layoutSearchResult.setVisibility(View.GONE);
        hideSuggestions();

        txtSearchMessage.setVisibility(View.VISIBLE);
        txtSearchMessage.setText(message);

        if (isError) {
            txtSearchMessage.setTextColor(getResources().getColor(R.color.error_red));
        } else {
            txtSearchMessage.setTextColor(getResources().getColor(R.color.gray));
        }
    }

    private void searchWordFromApi(String searchWord) {
        String cleanWord = searchWord.trim().toLowerCase();

        showLoadingState();

        dictionaryApiService.searchWord(cleanWord).enqueue(new Callback<List<DictionaryResponse>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<DictionaryResponse>> call,
                    @NonNull Response<List<DictionaryResponse>> response
            ) {
                if (!isAdded()) {
                    return;
                }

                progressSearchLoading.setVisibility(View.GONE);

                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().isEmpty()) {
                    currentApiWord = null;
                    showMessage(getString(R.string.no_result_found), true);
                    return;
                }

                DictionaryResponse dictionaryResponse = response.body().get(0);
                Word word = mapResponseToWord(dictionaryResponse, cleanWord);

                currentApiWord = word;
                showWordResult(word);
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<DictionaryResponse>> call,
                    @NonNull Throwable t
            ) {
                if (!isAdded()) {
                    return;
                }

                currentApiWord = null;
                progressSearchLoading.setVisibility(View.GONE);
                showMessage(getString(R.string.dictionary_api_error), true);
            }
        });
    }

    private Word mapResponseToWord(DictionaryResponse response, String fallbackWord) {
        String wordText = response.getWord();

        if (wordText == null || wordText.trim().isEmpty()) {
            wordText = fallbackWord;
        }

        String ipa = getBestIpa(response);
        String type = "";
        String definition = getString(R.string.no_definition_found);
        String example = getString(R.string.no_example_available);

        if (response.getMeanings() != null && !response.getMeanings().isEmpty()) {
            DictionaryResponse.Meaning meaning = response.getMeanings().get(0);

            if (meaning.getPartOfSpeech() != null) {
                type = meaning.getPartOfSpeech();
            }

            if (meaning.getDefinitions() != null && !meaning.getDefinitions().isEmpty()) {
                DictionaryResponse.Definition firstDefinition = meaning.getDefinitions().get(0);

                if (firstDefinition.getDefinition() != null
                        && !firstDefinition.getDefinition().trim().isEmpty()) {
                    definition = firstDefinition.getDefinition();
                }

                if (firstDefinition.getExample() != null
                        && !firstDefinition.getExample().trim().isEmpty()) {
                    example = firstDefinition.getExample();
                }
            }
        }

        String safeIdWord = wordText.toLowerCase()
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_]", "");

        Word word = new Word();
        word.setWordId("api_" + safeIdWord);
        word.setTopicId("api_search");
        word.setWord(wordText);
        word.setIpa(ipa);
        word.setMeaning(definition);
        word.setExample(example);
        word.setType(type);

        return word;
    }

    private String getBestIpa(DictionaryResponse response) {
        if (response.getPhonetic() != null
                && !response.getPhonetic().trim().isEmpty()) {
            return response.getPhonetic();
        }

        if (response.getPhonetics() != null) {
            for (DictionaryResponse.Phonetic phonetic : response.getPhonetics()) {
                if (phonetic.getText() != null
                        && !phonetic.getText().trim().isEmpty()) {
                    return phonetic.getText();
                }
            }
        }

        return "/ /";
    }

    private void showWordResult(Word word) {
        progressSearchLoading.setVisibility(View.GONE);
        txtSearchMessage.setVisibility(View.GONE);
        layoutSearchResult.setVisibility(View.VISIBLE);
        hideSuggestions();

        txtSearchWord.setText(word.getWord());
        txtSearchIpa.setText(word.getIpa());
        String languageCode = LocaleHelper.getSavedLanguage(requireContext());
        txtSearchDefinition.setText(word.getMeaningByLanguage(languageCode));
        txtSearchExample.setText(word.getExample());

        updateFavoriteIcon(word);
    }

    private void updateFavoriteIcon(Word word) {
        if (word == null || word.getWordId() == null) {
            imgSearchFavorite.setColorFilter(getResources().getColor(R.color.gray));
            return;
        }

        if (favoriteRepository.isWordSavedInAnyList(word.getWordId())) {
            imgSearchFavorite.setColorFilter(getResources().getColor(R.color.error_red));
        } else {
            imgSearchFavorite.setColorFilter(getResources().getColor(R.color.gray));
        }
    }

    private void showSaveWordToListDialog(Word word) {
        if (word == null || word.getWordId() == null) {
            return;
        }

        List<WordList> lists = favoriteRepository.getAllWordLists();

        if (lists.isEmpty()) {
            savedWordRepository.saveWord(word);

            WordList defaultList = favoriteRepository.createWordList(
                    getString(R.string.default_review_list)
            );

            favoriteRepository.addWordToList(defaultList.getListId(), word.getWordId());

            Toast.makeText(
                    requireContext(),
                    getString(R.string.saved_to_format, getString(R.string.default_review_list)),
                    Toast.LENGTH_SHORT
            ).show();

            updateFavoriteIcon(word);
            return;
        }

        Dialog dialog = new Dialog(requireContext());
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
                            requireContext(),
                            getString(R.string.removed_from_format, list.getListName()),
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    savedWordRepository.saveWord(word);

                    favoriteRepository.addWordToList(list.getListId(), word.getWordId());

                    Toast.makeText(
                            requireContext(),
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

    private void showCreateListAndSaveWordDialog(Word word) {
        if (word == null || word.getWordId() == null) {
            return;
        }

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

            savedWordRepository.saveWord(word);

            WordList newList = favoriteRepository.createWordList(listName);

            favoriteRepository.addWordToList(
                    newList.getListId(),
                    word.getWordId()
            );

            Toast.makeText(
                    requireContext(),
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
        TextView row = new TextView(requireContext());

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
        TextView row = new TextView(requireContext());

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

    @Override
    public void onResume() {
        super.onResume();

        if (currentApiWord != null) {
            updateFavoriteIcon(currentApiWord);
        }
    }

    @Override
    public void onDestroyView() {
        if (currentSuggestionCall != null) {
            currentSuggestionCall.cancel();
        }

        suggestionHandler.removeCallbacksAndMessages(null);

        if (ttsManager != null) {
            ttsManager.shutdown();
        }

        super.onDestroyView();
    }
}