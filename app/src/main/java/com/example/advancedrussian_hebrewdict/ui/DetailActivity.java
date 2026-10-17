package com.example.advancedrussian_hebrewdict.ui;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.advancedrussian_hebrewdict.R;
import com.example.advancedrussian_hebrewdict.data.AppDatabase;
import com.example.advancedrussian_hebrewdict.data.Word;
import com.example.advancedrussian_hebrewdict.util.LanguageManager;
import com.example.advancedrussian_hebrewdict.util.SpeechHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailActivity extends BaseActivity {
    private static final String TAG = "DetailActivity";
    private static final int REQUEST_EDIT = 100;

    private TextView tvNative, tvTarget, tvComment, tvGender, tvSpeechStatus, tvLearnedStatus;
    private ImageButton btnSave;
    private Button btnEdit, btnDelete, btnResetLearned;
    private ImageButton btnSpeak;
    private LinearLayout learnedSection;
    private ProgressBar speechProgress;
    private AppDatabase db;
    private Word currentWord;
    private SpeechHelper speechHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        db = AppDatabase.getInstance(this);
        speechHelper = new SpeechHelper(this);

        tvNative = findViewById(R.id.detail_russian);
        tvTarget = findViewById(R.id.detail_hebrew);
        tvComment = findViewById(R.id.detail_comment);
        tvGender = findViewById(R.id.detail_gender);
        tvLearnedStatus = findViewById(R.id.detail_learned_status);
        learnedSection = findViewById(R.id.learned_section);
        btnSave = findViewById(R.id.btn_save);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        btnSpeak = findViewById(R.id.btn_speak);
        btnResetLearned = findViewById(R.id.btn_reset_learned);
        speechProgress = findViewById(R.id.speech_progress);
        tvSpeechStatus = findViewById(R.id.speech_status);

        btnEdit.setText(R.string.edit);
        btnDelete.setText(R.string.delete_short);
        btnSpeak.setContentDescription(getString(R.string.speak));
        btnResetLearned.setText(R.string.reset);

        int wordId = getIntent().getIntExtra("word_id", -1);

        if (wordId > 0) {
            executor.execute(() -> {
                currentWord = db.wordDao().getWordById(wordId);
                runOnUiThread(() -> {
                    if (currentWord == null) { finish(); return; }
                    setupUI();
                });
            });
        } else {
            String target = getIntent().getStringExtra("hebrew");
            String nativeStr = getIntent().getStringExtra("russian");
            String comment = getIntent().getStringExtra("comment");
            String gender = getIntent().getStringExtra("gender");

            if (nativeStr == null || target == null) { finish(); return; }

            String targetCode = LanguageManager.getTargetCode(this);
            currentWord = new Word(target, nativeStr, comment);
            currentWord.id = 0;
            currentWord.isSaved = false;
            currentWord.gender = gender;
            currentWord.languageCode = targetCode;
            setupUI();
        }
    }

    private void setupUI() {
        displayWord();
        updateButtonsVisibility();

        btnSave.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, EditWordActivity.class);
            intent.putExtra("russian", currentWord.russian);
            intent.putExtra("hebrew", currentWord.hebrew);
            intent.putExtra("comment", currentWord.comment);
            intent.putExtra("gender", currentWord.gender);
            startActivityForResult(intent, REQUEST_EDIT);
        });

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, EditWordActivity.class);
            intent.putExtra("word_id", currentWord.id);
            startActivityForResult(intent, REQUEST_EDIT);
        });

        btnDelete.setOnClickListener(v -> deleteSavedWord());
        btnSpeak.setOnClickListener(v -> handleSpeakClick());
        btnResetLearned.setOnClickListener(v -> resetLearned());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT && resultCode == RESULT_OK) {
            finish();
        }
    }

    private void resetLearned() {
        currentWord.isLearned = false;
        currentWord.correctStreak = 0;
        final Word toSave = currentWord;
        final int folderId = currentWord.folderId;
        executor.execute(() -> {
            db.wordDao().update(toSave);
            db.folderDao().updateTimestamp(folderId, System.currentTimeMillis());
            runOnUiThread(this::updateLearnedUI);
        });
    }

    private void deleteSavedWord() {
        if (currentWord == null || currentWord.id <= 0) return;
        final int idToDelete = currentWord.id;
        final int folderId = currentWord.folderId;
        executor.execute(() -> {
            try {
                db.wordDao().deleteById(idToDelete);
                db.folderDao().updateTimestamp(folderId, System.currentTimeMillis());
                runOnUiThread(() -> { setResult(RESULT_OK); finish(); });
            } catch (Exception e) {
                Log.e(TAG, "Delete error: " + e.getMessage(), e);
            }
        });
    }

    private void handleSpeakClick() {
        if (currentWord == null || currentWord.hebrew == null || currentWord.hebrew.isEmpty()) return;

        btnSpeak.setEnabled(false);
        speechProgress.setVisibility(View.VISIBLE);
        tvSpeechStatus.setText(R.string.loading_speech);
        tvSpeechStatus.setVisibility(View.VISIBLE);

        speechHelper.speakAndPlay(currentWord.hebrew, new SpeechHelper.SpeechCallback() {
            @Override
            public void onSuccess(byte[] audioData) {
                speechProgress.setVisibility(View.GONE);
                tvSpeechStatus.setVisibility(View.GONE);
                // Воспроизведение делает сам SpeechHelper через speakAndPlay
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnSpeak.setEnabled(true);
                    speechProgress.setVisibility(View.GONE);
                    tvSpeechStatus.setText(getString(R.string.speech_error, error));
                });
            }
        });
    }

    private void displayWord() {
        tvNative.setText(currentWord.russian);
        tvTarget.setText(currentWord.hebrew);

        if (currentWord.gender != null && !currentWord.gender.isEmpty()) {
            tvGender.setText(currentWord.gender);
            tvGender.setVisibility(View.VISIBLE);
        } else {
            tvGender.setVisibility(View.GONE);
        }

        if (currentWord.comment != null && !currentWord.comment.isEmpty()) {
            tvComment.setText(currentWord.comment);
            tvComment.setVisibility(View.VISIBLE);
        } else {
            tvComment.setVisibility(View.GONE);
        }

        updateLearnedUI();
    }

    private void updateLearnedUI() {
        if (currentWord.id <= 0) {
            learnedSection.setVisibility(View.GONE);
            return;
        }
        learnedSection.setVisibility(View.VISIBLE);

        if (currentWord.isLearned) {
            tvLearnedStatus.setText(R.string.learned);
            tvLearnedStatus.setTextColor(0xFF4CAF50);
            tvLearnedStatus.setTextSize(18);
            btnResetLearned.setVisibility(View.VISIBLE);
        } else {
            int streak = Math.min(currentWord.correctStreak, 5);
            tvLearnedStatus.setText(getString(R.string.learning_progress, streak));
            tvLearnedStatus.setTextColor(0xFF757575);
            tvLearnedStatus.setTextSize(14);
            btnResetLearned.setVisibility(View.GONE);
        }
    }

    private void updateButtonsVisibility() {
        boolean isSavedWord = currentWord.id > 0 && currentWord.isSaved;
        if (isSavedWord) {
            btnSave.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnSave.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechHelper != null) speechHelper.close();
        executor.shutdown();
    }
}