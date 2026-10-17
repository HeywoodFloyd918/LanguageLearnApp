package com.example.advancedrussian_hebrewdict.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.advancedrussian_hebrewdict.R;
import com.example.advancedrussian_hebrewdict.data.AppDatabase;
import com.example.advancedrussian_hebrewdict.data.Word;
import com.example.advancedrussian_hebrewdict.ui.adapter.WordAdapter;
import com.example.advancedrussian_hebrewdict.util.LanguageManager;
import com.example.advancedrussian_hebrewdict.util.SpeechHelper;
import com.example.advancedrussian_hebrewdict.util.TranslationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends BaseActivity {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_HIDE_SEARCH_WARNING = "hide_search_warning_v3";

    private EditText searchInput;
    private Button btnSearch;
    private RecyclerView recyclerView;
    private WordAdapter adapter;
    private AppDatabase db;
    private TranslationHelper translationHelper;
    private SpeechHelper speechHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ProgressBar progressBar;
    private TextView loadingText;
    private TextView foundLabel;
    private TextView errorText;

    private String baseLang;
    private String targetCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        baseLang = LanguageManager.getBaseLang(this);
        targetCode = LanguageManager.getTargetCode(this);
        LanguageManager.Language target = LanguageManager.fromCode(targetCode);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(target.flag + " " + target.displayName(baseLang));
        }

        progressBar = findViewById(R.id.progress_bar);
        loadingText = findViewById(R.id.loading_text);
        foundLabel = findViewById(R.id.found_label);
        errorText = findViewById(R.id.error_text);

        progressBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        foundLabel.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);

        db = AppDatabase.getInstance(this);
        translationHelper = new TranslationHelper();
        speechHelper = new SpeechHelper(this);

        searchInput = findViewById(R.id.search_input);
        btnSearch = findViewById(R.id.btn_search);
        recyclerView = findViewById(R.id.recycler_view_search);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Хинт: ввод на РОДНОМ языке пользователя (RU → «на русском», EN → «in English»)
        searchInput.setHint(R.string.search_hint_native);
        btnSearch.setText(R.string.search_button);

        adapter = new WordAdapter(new ArrayList<>(),
                word -> {
                    Intent intent = new Intent(SearchActivity.this, DetailActivity.class);
                    if (word.id > 0) {
                        intent.putExtra("word_id", word.id);
                    } else {
                        intent.putExtra("word_id", 0);
                        intent.putExtra("hebrew", word.hebrew);
                        intent.putExtra("russian", word.russian);
                        intent.putExtra("comment", word.comment);
                    }
                    startActivity(intent);
                },
                word -> {
                    if (word.hebrew != null && !word.hebrew.isEmpty()) {
                        speechHelper.speakAndPlay(word.hebrew, null);
                    }
                });
        recyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> performSearch(searchInput.getText().toString()));
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchInput.getText().toString());
                return true;
            }
            return false;
        });

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_HIDE_SEARCH_WARNING, false)) {
            showSearchWarningDialog(prefs);
        }
    }

    private void showSearchWarningDialog(SharedPreferences prefs) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, pad / 2);

        TextView msg = new TextView(this);
        msg.setText(R.string.search_tip_text);
        msg.setTextSize(15);
        msg.setLineSpacing(4, 1.1f);
        container.addView(msg);

        CheckBox cb = new CheckBox(this);
        cb.setText(R.string.do_not_remind);
        LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cbParams.topMargin = pad / 2;
        cb.setLayoutParams(cbParams);
        container.addView(cb);

        new AlertDialog.Builder(this)
                .setTitle(R.string.search_tip_title)
                .setView(container)
                .setPositiveButton(R.string.got_it, (dialog, which) -> {
                    if (cb.isChecked()) {
                        prefs.edit().putBoolean(KEY_HIDE_SEARCH_WARNING, true).apply();
                    }
                })
                .setCancelable(true)
                .show();
    }

    private void performSearch(String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            errorText.setText(R.string.enter_word_for_search);
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        errorText.setVisibility(View.GONE);
        foundLabel.setVisibility(View.GONE);
        adapter.updateList(new ArrayList<>());

        progressBar.setVisibility(View.VISIBLE);
        loadingText.setText(R.string.searching);
        loadingText.setVisibility(View.VISIBLE);

        translateAndShow(trimmed);
    }

    private void translateAndShow(String nativeWord) {
        translationHelper.translate(nativeWord, baseLang, targetCode,
                new TranslationHelper.TranslationCallback() {
                    @Override
                    public void onSuccess(List<String> translations) {
                        List<Word> tempWords = new ArrayList<>();
                        for (String translation : translations) {
                            Word tempWord = new Word(translation, nativeWord, null);
                            tempWord.id = 0;
                            tempWord.isSaved = false;
                            tempWord.languageCode = targetCode;
                            tempWords.add(tempWord);
                        }
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            loadingText.setVisibility(View.GONE);
                            showResults(tempWords);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            loadingText.setVisibility(View.GONE);
                            errorText.setText(getString(R.string.translation_error, error));
                            errorText.setVisibility(View.VISIBLE);
                        });
                    }
                });
    }

    private void showResults(List<Word> words) {
        adapter.updateList(words);
        foundLabel.setText(getString(R.string.found, words.size()));
        foundLabel.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (translationHelper != null) translationHelper.close();
        if (speechHelper != null) speechHelper.close();
        executor.shutdown();
    }
}