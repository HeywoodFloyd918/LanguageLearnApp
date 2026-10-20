package com.example.learn_dict_cards_languages_mila.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.learn_dict_cards_languages_mila.MainActivity;
import com.example.learn_dict_cards_languages_mila.R;
import com.example.learn_dict_cards_languages_mila.data.AppDatabase;
import com.example.learn_dict_cards_languages_mila.data.Folder;
import com.example.learn_dict_cards_languages_mila.data.Word;
import com.example.learn_dict_cards_languages_mila.util.LanguageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditWordActivity extends BaseActivity {

    private EditText etNative, etTarget, etComment;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale, rbNone;
    private Spinner spinnerFolder;
    private Button btnConfirmSave;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ProgressBar progressBar;
    private TextView statusText;

    private final List<Folder> availableFolders = new ArrayList<>();
    private Word editingWord = null;
    private boolean launchedFromFolder = false;

    private String baseLang;
    private String targetCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_word);

        db = AppDatabase.getInstance(this);
        baseLang = LanguageManager.getBaseLang(this);
        targetCode = LanguageManager.getTargetCode(this);
        LanguageManager.Language target = LanguageManager.fromCode(targetCode);

        etNative = findViewById(R.id.et_russian);
        etTarget = findViewById(R.id.et_hebrew);
        etComment = findViewById(R.id.et_comment);
        rgGender = findViewById(R.id.rg_gender);
        rbMale = findViewById(R.id.rb_male);
        rbFemale = findViewById(R.id.rb_female);
        rbNone = findViewById(R.id.rb_none);
        spinnerFolder = findViewById(R.id.spinner_folder);
        btnConfirmSave = findViewById(R.id.btn_confirm_save);
        progressBar = findViewById(R.id.edit_progress_bar);
        statusText = findViewById(R.id.edit_status_text);

        // Локальные заголовки под выбранные языки
        TextView title = findViewById(R.id.title_edit);
        if (title != null) {
            title.setText(getString(R.string.edit_word_title,
                    target.flag + " " + target.displayName(baseLang)));
        }
        TextView labelNative = findViewById(R.id.label_russian);
        if (labelNative != null) {
            labelNative.setText(getString(R.string.label_native));
        }
        TextView labelTarget = findViewById(R.id.label_hebrew);
        if (labelTarget != null) {
            switch (targetCode) {
                case "he": labelTarget.setText(R.string.label_target_he); break;
                case "en": labelTarget.setText(R.string.label_target_en); break;
                case "es": labelTarget.setText(R.string.label_target_es); break;
                default:   labelTarget.setText(R.string.label_target_en); break;
            }
        }
        if (etNative != null) etNative.setHint(R.string.hint_native);
        if (etTarget != null) {
            switch (targetCode) {
                case "he": etTarget.setHint(R.string.hint_target_he); break;
                case "en": etTarget.setHint(R.string.hint_target_en); break;
                case "es": etTarget.setHint(R.string.hint_target_es); break;
            }
        }
        TextView labelGender = findViewById(R.id.label_gender);
        if (labelGender != null) labelGender.setText(R.string.gender_label);
        rbMale.setText(R.string.gender_male);
        rbFemale.setText(R.string.gender_female);
        rbNone.setText(R.string.gender_none);
        TextView labelFolder = findViewById(R.id.label_folder);
        if (labelFolder != null) labelFolder.setText(R.string.folder_label2);
        TextView labelComment = findViewById(R.id.label_comment);
        if (labelComment != null) labelComment.setText(R.string.comment_label);
        etComment.setHint(R.string.comment_hint);

        int wordId = getIntent().getIntExtra("word_id", -1);
        int defaultFolderId = getIntent().getIntExtra("default_folder_id", -1);
        launchedFromFolder = defaultFolderId > 0;

        if (wordId > 0) {
            btnConfirmSave.setText(R.string.save_changes);
            executor.execute(() -> {
                Word existing = db.wordDao().getWordById(wordId);
                List<Folder> folders = db.folderDao().getAllFolders(targetCode);
                runOnUiThread(() -> {
                    if (existing == null) { finish(); return; }
                    editingWord = existing;
                    etNative.setText(existing.russian);
                    etTarget.setText(existing.hebrew);
                    if (existing.comment != null) etComment.setText(existing.comment);
                    if ("м".equals(existing.gender)) rbMale.setChecked(true);
                    else if ("ж".equals(existing.gender)) rbFemale.setChecked(true);
                    else rbNone.setChecked(true);
                    setupSpinner(folders, existing.folderId);
                });
            });
        } else {
            String nativeStr = getIntent().getStringExtra("russian");
            String targetStr = getIntent().getStringExtra("hebrew");
            String commentStr = getIntent().getStringExtra("comment");
            String gender = getIntent().getStringExtra("gender");

            if (nativeStr != null) etNative.setText(nativeStr);
            if (targetStr != null) etTarget.setText(targetStr);
            if (commentStr != null) etComment.setText(commentStr);

            if ("м".equals(gender)) rbMale.setChecked(true);
            else if ("ж".equals(gender)) rbFemale.setChecked(true);
            else rbNone.setChecked(true);

            executor.execute(() -> {
                List<Folder> folders = db.folderDao().getAllFolders(targetCode);
                int fallbackId;
                if (defaultFolderId > 0) {
                    fallbackId = defaultFolderId;
                } else {
                    Folder lastCreated = db.folderDao().getLastCreatedFolder(targetCode);
                    fallbackId = lastCreated != null ? lastCreated.id : -1;
                }
                final int selectedId = fallbackId;
                runOnUiThread(() -> setupSpinner(folders, selectedId));
            });
        }

        btnConfirmSave.setOnClickListener(v -> saveWord());
    }

    private void setupSpinner(List<Folder> folders, int selectedFolderId) {
        availableFolders.clear();
        availableFolders.addAll(folders);

        List<String> names = new ArrayList<>();
        for (Folder f : folders) names.add(f.name);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                EditWordActivity.this,
                android.R.layout.simple_spinner_item,
                names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFolder.setAdapter(adapter);

        for (int i = 0; i < folders.size(); i++) {
            if (folders.get(i).id == selectedFolderId) {
                spinnerFolder.setSelection(i);
                break;
            }
        }
    }

    private void saveWord() {
        String nativeText = etNative.getText().toString().trim();
        String targetText = etTarget.getText().toString().trim();
        String comment = etComment.getText().toString().trim();

        if (nativeText.isEmpty() || targetText.isEmpty()) {
            statusText.setText(R.string.fill_both_fields);
            statusText.setVisibility(View.VISIBLE);
            return;
        }

        if (availableFolders.isEmpty()) {
            statusText.setText(R.string.no_folders_for_lang);
            statusText.setVisibility(View.VISIBLE);
            return;
        }

        int selectedIndex = spinnerFolder.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= availableFolders.size()) {
            statusText.setText(R.string.choose_folder);
            statusText.setVisibility(View.VISIBLE);
            return;
        }
        int folderId = availableFolders.get(selectedIndex).id;

        String gender = null;
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        if (selectedGenderId == R.id.rb_male) gender = "м";
        else if (selectedGenderId == R.id.rb_female) gender = "ж";

        btnConfirmSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText(R.string.saving);
        statusText.setVisibility(View.VISIBLE);

        final String finalGender = gender;
        final int finalFolderId = folderId;

        executor.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                if (editingWord != null) {
                    int oldFolderId = editingWord.folderId;
                    editingWord.russian = nativeText;
                    editingWord.hebrew = targetText;
                    editingWord.comment = comment.isEmpty() ? null : comment;
                    editingWord.gender = finalGender;
                    editingWord.folderId = finalFolderId;
                    db.wordDao().update(editingWord);

                    db.folderDao().updateTimestamp(finalFolderId, now);
                    if (oldFolderId != finalFolderId) {
                        db.folderDao().updateTimestamp(oldFolderId, now);
                    }

                    runOnUiThread(() -> {
                        setResult(RESULT_OK);
                        finish();
                    });
                } else {
                    Word newWord = new Word(targetText, nativeText,
                            comment.isEmpty() ? null : comment);
                    newWord.id = 0;
                    newWord.isSaved = true;
                    newWord.gender = finalGender;
                    newWord.folderId = finalFolderId;
                    newWord.languageCode = targetCode;
                    newWord.sortOrder = db.wordDao().getMaxSortOrderInFolder(finalFolderId) + 1;
                    db.wordDao().insert(newWord);

                    db.folderDao().updateTimestamp(finalFolderId, now);

                    runOnUiThread(() -> {
                        if (launchedFromFolder) {
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Intent intent = new Intent(EditWordActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnConfirmSave.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    statusText.setText(getString(R.string.error_prefix, e.getMessage()));
                    statusText.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}