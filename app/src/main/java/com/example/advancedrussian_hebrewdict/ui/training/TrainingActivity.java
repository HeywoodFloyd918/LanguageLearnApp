package com.example.advancedrussian_hebrewdict.ui.training;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.advancedrussian_hebrewdict.R;
import com.example.advancedrussian_hebrewdict.data.AppDatabase;
import com.example.advancedrussian_hebrewdict.data.Folder;
import com.example.advancedrussian_hebrewdict.data.Word;
import com.example.advancedrussian_hebrewdict.ui.BaseActivity;
import com.example.advancedrussian_hebrewdict.util.GroqHelper;
import com.example.advancedrussian_hebrewdict.util.LanguageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrainingActivity extends BaseActivity {

    private static final int STREAK_TO_LEARN = 5;
    private static final int MIN_WORDS_FOR_TRAINING = 4;
    private static final int COLOR_DEFAULT_TEXT = 0xFF212121;
    private static final int COLOR_CORRECT = 0xFF4CAF50;
    private static final int COLOR_WRONG = 0xFFE57373;
    private static final int COLOR_INFO = 0xFF757575;
    private static final int COLOR_BUTTON_DEFAULT = 0xFFE0E0E0;

    private static final int FILTER_ALL = 0;
    private static final int FILTER_UNLEARNED = 1;
    private static final int FILTER_LEARNED = 2;

    private ScrollView setupPanel;
    private LinearLayout sessionPanel;
    private Spinner spinnerMode;
    private Spinner spinnerFilter;
    private TextView tvSelectedFolders;
    private TextView setupInfo;

    private TextView progressText;
    private TextView questionText;
    private TextView feedbackText;
    private Button[] optionButtons = new Button[4];
    private LinearLayout optionsContainer;
    private LinearLayout writeContainer;
    private EditText etAnswer;
    private Button btnCheckAnswer;
    private ImageButton btnVoiceToggle;
    private LinearLayout btnReplay;
    private ProgressBar generationProgress;
    private LinearLayout btnShowRussianContainer;
    private TextView russianTranslation;

    private AppDatabase db;
    private TrainingPreferences prefs;
    private TrainingSession session;
    private SpeechManager speechManager;
    private GroqHelper groqHelper;
    private FolderSelectionDialog folderDialog;

    private String baseLang;
    private String targetCode;
    private LanguageManager.Language targetLang;

    private final List<Folder> availableFolders = new ArrayList<>();
    private final Set<Integer> selectedFolderIds = new HashSet<>();
    private boolean selectAllFolders = true;

    private TrainingMode currentMode = TrainingMode.CHOICE;
    private int currentFilter = FILTER_ALL;

    private Word currentWord;
    private int correctButtonIndex = -1;
    private boolean questionInBase;
    private boolean isShowingAnswer = false;

    private String currentSentence = null;
    private String currentMissingWord = null;
    private String currentTranslation = null;
    private boolean isGeneratingSentence = false;
    private List<String> chosenDistractors = null;

    private int avoidWordId = -1;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        baseLang = LanguageManager.getBaseLang(this);
        targetCode = LanguageManager.getTargetCode(this);
        targetLang = LanguageManager.fromCode(targetCode);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.training_title,
                    targetLang.flag + " " + targetLang.displayName(baseLang)));
        }

        db = AppDatabase.getInstance(this);
        prefs = new TrainingPreferences(this);
        session = new TrainingSession();
        speechManager = new SpeechManager(this);
        groqHelper = new GroqHelper();
        folderDialog = new FolderSelectionDialog(this);

        bindViews();
        setupModeSpinner();
        setupFilterSpinner();
        setupListeners();

        loadFolders();
    }

    private void bindViews() {
        setupPanel = findViewById(R.id.setup_panel);
        sessionPanel = findViewById(R.id.session_panel);
        spinnerMode = findViewById(R.id.spinner_mode);
        spinnerFilter = findViewById(R.id.spinner_filter);
        tvSelectedFolders = findViewById(R.id.tv_selected_folders);
        setupInfo = findViewById(R.id.setup_info);

        progressText = findViewById(R.id.training_progress);
        questionText = findViewById(R.id.training_question);
        feedbackText = findViewById(R.id.training_feedback);

        optionsContainer = findViewById(R.id.options_container);
        writeContainer = findViewById(R.id.write_container);
        etAnswer = findViewById(R.id.et_answer);
        btnCheckAnswer = findViewById(R.id.btn_check_answer);

        optionButtons[0] = findViewById(R.id.option_1);
        optionButtons[1] = findViewById(R.id.option_2);
        optionButtons[2] = findViewById(R.id.option_3);
        optionButtons[3] = findViewById(R.id.option_4);

        btnVoiceToggle = findViewById(R.id.btn_voice_toggle);
        btnReplay = findViewById(R.id.btn_replay);
        generationProgress = findViewById(R.id.generation_progress);
        btnShowRussianContainer = findViewById(R.id.btn_show_russian_container);
        russianTranslation = findViewById(R.id.russian_translation);
    }

    private void setupModeSpinner() {
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                TrainingMode.displayNames(this));
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(modeAdapter);
    }

    private void setupFilterSpinner() {
        String[] filterNames = {
                getString(R.string.filter_all),
                getString(R.string.filter_unlearned),
                getString(R.string.filter_learned)
        };
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, filterNames);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);

        int savedFilter = prefs.getLastFilterIndex();
        if (savedFilter >= 0 && savedFilter < filterNames.length) {
            spinnerFilter.setSelection(savedFilter);
        }
    }

    private void setupListeners() {
        btnVoiceToggle.setImageResource(R.drawable.ic_volume_on);
        btnVoiceToggle.setOnClickListener(v -> {
            speechManager.toggleVoice();
            btnVoiceToggle.setImageResource(speechManager.isVoiceEnabled()
                    ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);
        });

        btnReplay.setOnClickListener(v -> replayCurrent());
        btnShowRussianContainer.setOnClickListener(v -> toggleTranslation());
        findViewById(R.id.btn_select_folders).setOnClickListener(v -> openFolderDialog());
        findViewById(R.id.btn_start_training).setOnClickListener(v -> startTraining());
        findViewById(R.id.btn_exit_training).setOnClickListener(v -> finish());

        btnCheckAnswer.setOnClickListener(v -> handleWriteAnswer());
        etAnswer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleWriteAnswer();
                return true;
            }
            return false;
        });

        TextView showNative = findViewById(R.id.btn_show_russian_text);
        if (showNative != null) showNative.setText(R.string.show_in_native);
    }

    private void loadFolders() {
        executor.execute(() -> {
            List<Folder> folders = db.folderDao().getAllFoldersNewestFirst(targetCode);
            runOnUiThread(() -> {
                availableFolders.clear();
                availableFolders.addAll(folders);

                int savedMode = prefs.getLastModeIndex();
                if (savedMode >= 0 && savedMode < spinnerMode.getAdapter().getCount()) {
                    spinnerMode.setSelection(savedMode);
                }

                selectAllFolders = prefs.isLastSelectAll();
                selectedFolderIds.clear();
                selectedFolderIds.addAll(prefs.getLastFolderIds());
                updateSelectedFoldersText();
            });
        });
    }

    private void openFolderDialog() {
        if (availableFolders.isEmpty()) {
            Toast.makeText(this, R.string.folders_none, Toast.LENGTH_SHORT).show();
            return;
        }
        folderDialog.show(availableFolders, selectAllFolders, selectedFolderIds,
                (selAll, ids) -> {
                    selectAllFolders = selAll;
                    selectedFolderIds.clear();
                    selectedFolderIds.addAll(ids);
                    if (!availableFolders.isEmpty()
                            && selectedFolderIds.size() == availableFolders.size()) {
                        selectAllFolders = true;
                    }
                    updateSelectedFoldersText();
                    saveSettings();
                });
    }

    private void updateSelectedFoldersText() {
        if (availableFolders.isEmpty()) {
            tvSelectedFolders.setText(R.string.folders_none);
        } else if (selectAllFolders) {
            tvSelectedFolders.setText(R.string.folders_all);
        } else if (selectedFolderIds.isEmpty()) {
            tvSelectedFolders.setText(R.string.folders_none);
        } else if (selectedFolderIds.size() == 1) {
            for (Folder f : availableFolders) {
                if (selectedFolderIds.contains(f.id)) {
                    tvSelectedFolders.setText(getString(R.string.folders_one, f.name));
                    return;
                }
            }
        } else {
            tvSelectedFolders.setText(getString(R.string.folders_n, selectedFolderIds.size()));
        }
    }

    private void saveSettings() {
        prefs.saveTrainingSettings(
                spinnerMode.getSelectedItemPosition(),
                spinnerFilter.getSelectedItemPosition(),
                selectAllFolders,
                selectedFolderIds);
    }

    private void startTraining() {
        currentMode = TrainingMode.fromIndex(spinnerMode.getSelectedItemPosition());
        currentFilter = spinnerFilter.getSelectedItemPosition();

        if (!selectAllFolders && selectedFolderIds.isEmpty()) {
            setupInfo.setText(R.string.choose_one_folder);
            setupInfo.setVisibility(View.VISIBLE);
            return;
        }

        saveSettings();

        executor.execute(() -> {
            List<Word> rawWords = new ArrayList<>();
            if (selectAllFolders) {
                rawWords = db.wordDao().getAllSaved(targetCode);
            } else {
                for (Integer folderId : selectedFolderIds) {
                    rawWords.addAll(db.wordDao().getWordsInFolder(targetCode, folderId));
                }
            }

            final List<Word> finalWords = applyFilter(rawWords, currentFilter);

            runOnUiThread(() -> {
                if (finalWords.size() < MIN_WORDS_FOR_TRAINING) {
                    setupInfo.setText(getString(R.string.need_more_words,
                            MIN_WORDS_FOR_TRAINING, finalWords.size()));
                    setupInfo.setVisibility(View.VISIBLE);
                    return;
                }
                session.setWords(finalWords);
                session.resetHistory();
                setupPanel.setVisibility(View.GONE);
                sessionPanel.setVisibility(View.VISIBLE);

                if (currentMode.isChoiceMode()) {
                    optionsContainer.setVisibility(View.VISIBLE);
                    writeContainer.setVisibility(View.GONE);
                } else {
                    optionsContainer.setVisibility(View.GONE);
                    writeContainer.setVisibility(View.VISIBLE);
                }

                avoidWordId = -1;
                nextQuestion();
            });
        });
    }

    private List<Word> applyFilter(List<Word> words, int filter) {
        if (filter == FILTER_ALL) return words;
        List<Word> result = new ArrayList<>();
        for (Word w : words) {
            if (filter == FILTER_UNLEARNED && !w.isLearned) result.add(w);
            else if (filter == FILTER_LEARNED && w.isLearned) result.add(w);
        }
        return result;
    }

    private void nextQuestion() {
        resetQuestionState();

        if (session.isEmpty()) { finish(); return; }

        currentWord = session.pickWeightedRandom(avoidWordId);
        avoidWordId = -1;
        if (currentWord == null) { finish(); return; }

        if (currentMode.needsSentence()) {
            generateSentenceAndShow();
            return;
        }

        switch (currentMode) {
            case WRITE_TARGET: questionInBase = true; break;
            case WRITE_NATIVE: questionInBase = false; break;
            default: questionInBase = session.getRandom().nextBoolean();
        }

        String display = questionInBase ? currentWord.russian : currentWord.hebrew;
        questionText.setText(display);

        if (currentMode.isChoiceMode()) {
            showChoiceOptions();
        } else {
            showWriteInput();
        }

        if (!questionInBase) speechManager.speakWordAsync(currentWord.hebrew);

        updateProgressText();
    }

    private void resetQuestionState() {
        isShowingAnswer = false;
        feedbackText.setVisibility(View.INVISIBLE);
        feedbackText.setText("");
        feedbackText.setTextColor(COLOR_WRONG);
        currentSentence = null;
        currentMissingWord = null;
        currentTranslation = null;
        chosenDistractors = null;

        russianTranslation.setText("");
        russianTranslation.setVisibility(View.GONE);
        btnShowRussianContainer.setVisibility(View.GONE);
        generationProgress.setVisibility(View.GONE);
        questionText.setVisibility(View.VISIBLE);
    }

    private void generateSentenceAndShow() {
        if (isGeneratingSentence) return;
        isGeneratingSentence = true;

        generationProgress.setVisibility(View.VISIBLE);
        questionText.setVisibility(View.GONE);
        btnShowRussianContainer.setVisibility(View.GONE);
        russianTranslation.setVisibility(View.GONE);
        progressText.setText(R.string.ai_request);

        final List<String> candidates = currentMode == TrainingMode.FILL_BLANK_CHOICE
                ? session.collectCandidates(currentWord) : null;

        groqHelper.generateSentence(currentWord.hebrew, currentWord.russian,
                targetCode, baseLang, candidates,
                new GroqHelper.SentenceCallback() {
                    @Override
                    public void onSuccess(String sentence, String missingWord,
                                          String translation, List<String> distractors) {
                        isGeneratingSentence = false;
                        currentSentence = sentence;
                        currentMissingWord = missingWord;
                        currentTranslation = translation;

                        if (currentMode == TrainingMode.FILL_BLANK_CHOICE) {
                            chosenDistractors = validateAndFillDistractors(distractors, missingWord);
                        }

                        runOnUiThread(() -> {
                            generationProgress.setVisibility(View.GONE);
                            questionText.setVisibility(View.VISIBLE);
                            questionText.setText(sentence);
                            btnShowRussianContainer.setVisibility(View.VISIBLE);
                            updateProgressText();

                            if (currentMode == TrainingMode.FILL_BLANK_CHOICE) {
                                showBlankChoiceOptions();
                            } else {
                                showWriteInputForBlank();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        isGeneratingSentence = false;
                        if (currentWord != null) avoidWordId = currentWord.id;
                        runOnUiThread(() -> {
                            generationProgress.setVisibility(View.GONE);
                            questionText.setVisibility(View.VISIBLE);
                            feedbackText.setTextColor(COLOR_INFO);
                            feedbackText.setText(R.string.ai_failed);
                            feedbackText.setVisibility(View.VISIBLE);
                            handler.postDelayed(TrainingActivity.this::nextQuestion, 2000);
                        });
                    }
                });
    }

    private List<String> validateAndFillDistractors(List<String> fromGroq, String missingWord) {
        List<String> cleaned = new ArrayList<>();
        if (fromGroq != null) {
            Set<String> seen = new HashSet<>();
            for (String d : fromGroq) {
                if (d == null) continue;
                String trimmed = d.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.equals(missingWord)) continue;
                if (seen.contains(trimmed)) continue;
                seen.add(trimmed);
                cleaned.add(trimmed);
            }
        }
        if (cleaned.size() < 3) {
            cleaned.addAll(session.pickRandomDistractors(
                    currentWord, cleaned, missingWord, 3 - cleaned.size()));
        }
        return cleaned;
    }

    private void showChoiceOptions() {
        List<Word> others = new ArrayList<>();
        for (Word w : session.getWords()) if (w.id != currentWord.id) others.add(w);
        Collections.shuffle(others, session.getRandom());

        List<Word> distractors = new ArrayList<>();
        for (Word w : others) {
            if (distractors.size() >= 3) break;
            String key = questionInBase ? w.hebrew : w.russian;
            if (key == null) continue;
            String correctKey = questionInBase ? currentWord.hebrew : currentWord.russian;
            if (key.equals(correctKey)) continue;
            boolean dup = false;
            for (Word d : distractors) {
                String dKey = questionInBase ? d.hebrew : d.russian;
                if (key.equals(dKey)) { dup = true; break; }
            }
            if (!dup) distractors.add(w);
        }

        List<Word> options = new ArrayList<>(distractors);
        options.add(currentWord);
        Collections.shuffle(options, session.getRandom());

        correctButtonIndex = -1;
        for (int i = 0; i < optionButtons.length; i++) {
            if (i < options.size()) {
                Word w = options.get(i);
                String text = questionInBase ? w.hebrew : w.russian;
                optionButtons[i].setText(text);
                optionButtons[i].setVisibility(View.VISIBLE);
                optionButtons[i].setEnabled(true);
                optionButtons[i].setBackgroundColor(COLOR_BUTTON_DEFAULT);
                final Word chosen = w;
                final Button clicked = optionButtons[i];
                optionButtons[i].setOnClickListener(v -> handleChoiceAnswer(chosen, clicked));
                if (w.id == currentWord.id) correctButtonIndex = i;
            } else {
                optionButtons[i].setVisibility(View.GONE);
            }
        }
    }

    private void showBlankChoiceOptions() {
        List<String> options = new ArrayList<>();
        options.add(currentMissingWord);
        if (chosenDistractors != null) {
            for (String d : chosenDistractors) {
                if (!d.equals(currentMissingWord) && !options.contains(d)) options.add(d);
            }
        }
        if (options.size() < 4) {
            options.addAll(session.pickRandomDistractors(
                    currentWord, options, currentMissingWord, 4 - options.size()));
        }
        Collections.shuffle(options, session.getRandom());

        correctButtonIndex = -1;
        for (int i = 0; i < optionButtons.length; i++) {
            if (i < options.size()) {
                optionButtons[i].setText(options.get(i));
                optionButtons[i].setVisibility(View.VISIBLE);
                optionButtons[i].setEnabled(true);
                optionButtons[i].setBackgroundColor(COLOR_BUTTON_DEFAULT);
                final String chosen = options.get(i);
                final Button clicked = optionButtons[i];
                optionButtons[i].setOnClickListener(v -> handleBlankChoiceAnswer(chosen, clicked));
                if (options.get(i).equals(currentMissingWord)) correctButtonIndex = i;
            } else {
                optionButtons[i].setVisibility(View.GONE);
            }
        }
    }

    private void showWriteInputForBlank() {
        prepareWriteInput(getString(R.string.enter_missing_word));
    }

    private void showWriteInput() {
        prepareWriteInput(questionInBase
                ? getString(R.string.enter_in_target)
                : getString(R.string.enter_in_native));
    }

    private void prepareWriteInput(String hint) {
        etAnswer.setEnabled(true);
        etAnswer.setText("");
        etAnswer.setTextColor(COLOR_DEFAULT_TEXT);
        btnCheckAnswer.setEnabled(true);
        etAnswer.setInputType(InputType.TYPE_CLASS_TEXT);
        etAnswer.setHint(hint);

        etAnswer.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etAnswer, InputMethodManager.SHOW_IMPLICIT);
    }

    private void updateProgressText() {
        int learned = session.countLearned();
        int streak = Math.min(currentWord.correctStreak, STREAK_TO_LEARN);
        progressText.setText(getString(R.string.progress_label,
                streak, STREAK_TO_LEARN, learned, session.size()));
    }

    private void handleChoiceAnswer(Word chosen, Button clicked) {
        if (isShowingAnswer) return;
        isShowingAnswer = true;
        for (Button b : optionButtons) b.setEnabled(false);

        if (chosen.id == currentWord.id) {
            clicked.setBackgroundColor(COLOR_CORRECT);
            speechManager.playSuccess();
            applyCorrectAnswer();
            executor.execute(() -> db.wordDao().update(currentWord));
            speechManager.speakWordAndWait(currentWord.hebrew,
                    () -> handler.postDelayed(this::nextQuestion, 800));
        } else {
            clicked.setBackgroundColor(COLOR_WRONG);
            if (correctButtonIndex >= 0) {
                optionButtons[correctButtonIndex].setBackgroundColor(COLOR_CORRECT);
            }
            speechManager.playError();
            applyWrongAnswer(getString(R.string.series_reset));
            executor.execute(() -> db.wordDao().update(currentWord));
            speechManager.speakWordAndWait(currentWord.hebrew,
                    () -> handler.postDelayed(this::nextQuestion, 1200));
        }
    }

    private void handleBlankChoiceAnswer(String chosen, Button clicked) {
        if (isShowingAnswer) return;
        isShowingAnswer = true;
        for (Button b : optionButtons) b.setEnabled(false);

        if (chosen.equals(currentMissingWord)) {
            clicked.setBackgroundColor(COLOR_CORRECT);
            speechManager.playSuccess();
            applyCorrectAnswer();
            speechManager.speakWordAndWait(currentMissingWord,
                    () -> handler.postDelayed(this::nextQuestion, 800));
        } else {
            clicked.setBackgroundColor(COLOR_WRONG);
            if (correctButtonIndex >= 0) {
                optionButtons[correctButtonIndex].setBackgroundColor(COLOR_CORRECT);
            }
            speechManager.playError();
            applyWrongAnswer(getString(R.string.correct_answer, currentMissingWord));
            speechManager.speakWordAndWait(currentMissingWord,
                    () -> handler.postDelayed(this::nextQuestion, 1500));
        }
        executor.execute(() -> db.wordDao().update(currentWord));
    }

    private void handleWriteAnswer() {
        if (isShowingAnswer) return;
        String userInput = etAnswer.getText().toString().trim();
        if (userInput.isEmpty()) return;

        isShowingAnswer = true;
        btnCheckAnswer.setEnabled(false);
        etAnswer.setEnabled(false);

        if (currentMode == TrainingMode.FILL_BLANK_WRITE) {
            boolean correct = normalize(userInput).equals(normalize(currentMissingWord));
            if (correct) {
                etAnswer.setTextColor(COLOR_CORRECT);
                speechManager.playSuccess();
                applyCorrectAnswer();
            } else {
                etAnswer.setTextColor(COLOR_WRONG);
                speechManager.playError();
                applyWrongAnswer(getString(R.string.correct_answer, currentMissingWord));
            }
            executor.execute(() -> db.wordDao().update(currentWord));
            speechManager.speakWordAndWait(currentMissingWord,
                    () -> handler.postDelayed(this::nextQuestion, correct ? 800 : 1500));
            return;
        }

        String correctAnswer = questionInBase ? currentWord.hebrew : currentWord.russian;
        boolean correct = normalize(userInput).equals(normalize(correctAnswer));

        if (correct) {
            etAnswer.setTextColor(COLOR_CORRECT);
            speechManager.playSuccess();
            applyCorrectAnswer();
            executor.execute(() -> db.wordDao().update(currentWord));
            speechManager.speakWordAndWait(currentWord.hebrew,
                    () -> handler.postDelayed(this::nextQuestion, 800));
        } else {
            etAnswer.setTextColor(COLOR_WRONG);
            speechManager.playError();
            applyWrongAnswer(getString(R.string.correct_answer, correctAnswer));
            executor.execute(() -> db.wordDao().update(currentWord));
            speechManager.speakWordAndWait(currentWord.hebrew,
                    () -> handler.postDelayed(this::nextQuestion, 1500));
        }
    }

    private void applyCorrectAnswer() {
        if (currentWord.correctStreak < STREAK_TO_LEARN) {
            currentWord.correctStreak++;
        }
        if (currentWord.correctStreak >= STREAK_TO_LEARN && !currentWord.isLearned) {
            currentWord.isLearned = true;
            Toast.makeText(this, getString(R.string.word_learned_toast,
                    currentWord.russian), Toast.LENGTH_LONG).show();
        }
    }

    private void applyWrongAnswer(String message) {
        currentWord.correctStreak = 0;
        feedbackText.setTextColor(COLOR_WRONG);
        feedbackText.setText(message);
        feedbackText.setVisibility(View.VISIBLE);
    }

    private String normalize(String s) {
        return s.trim().toLowerCase()
                .replace("?", "").replace("!", "")
                .replace(".", "").replace(",", "")
                .replace("־", " ");
    }

    private void replayCurrent() {
        if (currentMode.needsSentence() && currentSentence != null) {
            speechManager.speakSentenceAsync(currentSentence);
        } else if (currentWord != null) {
            speechManager.speakWordAsync(currentWord.hebrew);
        }
    }

    private void toggleTranslation() {
        if (currentTranslation == null || currentTranslation.isEmpty()) {
            Toast.makeText(this, R.string.ai_request, Toast.LENGTH_SHORT).show();
            return;
        }
        if (russianTranslation.getVisibility() == View.VISIBLE) {
            russianTranslation.setVisibility(View.GONE);
            return;
        }
        if (!currentTranslation.contains("___")) {
            russianTranslation.setText(R.string.translation_not_available);
            russianTranslation.setTextColor(0xFF9E9E9E);
            russianTranslation.setVisibility(View.VISIBLE);
            return;
        }
        russianTranslation.setText(currentTranslation);
        russianTranslation.setTextColor(0xFF757575);
        russianTranslation.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (speechManager != null) speechManager.release();
        if (groqHelper != null) groqHelper.close();
        executor.shutdown();
    }
}