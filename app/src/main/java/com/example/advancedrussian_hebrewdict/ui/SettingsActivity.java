package com.example.advancedrussian_hebrewdict.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.advancedrussian_hebrewdict.MainActivity;
import com.example.advancedrussian_hebrewdict.R;
import com.example.advancedrussian_hebrewdict.data.AppDatabase;
import com.example.advancedrussian_hebrewdict.data.Folder;
import com.example.advancedrussian_hebrewdict.data.StarterData;
import com.example.advancedrussian_hebrewdict.data.Word;
import com.example.advancedrussian_hebrewdict.util.LanguageManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_SPEAK_ON_CARD = "speak_on_card";
    private static final int REQUEST_IMPORT = 200;

    private LinearLayout btnExport, btnImport, btnResetAllProgress, btnLanguage, btnBaseLanguage, btnAddStarters;
    private ProgressBar exportProgress;
    private TextView exportStatus, tvCurrentLanguage, tvCurrentBaseLanguage;
    private Switch switchSpeakCard;
    private AppDatabase db;
    private SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.settings_title);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        db = AppDatabase.getInstance(this);

        btnExport = findViewById(R.id.btn_export);
        btnImport = findViewById(R.id.btn_import);
        btnResetAllProgress = findViewById(R.id.btn_reset_all_progress);
        btnLanguage = findViewById(R.id.btn_language);
        btnBaseLanguage = findViewById(R.id.btn_base_language);
        btnAddStarters = findViewById(R.id.btn_add_starters);
        tvCurrentLanguage = findViewById(R.id.tv_current_language);
        tvCurrentBaseLanguage = findViewById(R.id.tv_current_base_language);
        exportProgress = findViewById(R.id.export_progress);
        exportStatus = findViewById(R.id.export_status);
        switchSpeakCard = findViewById(R.id.switch_speak_card);

        boolean speakOnCard = prefs.getBoolean(KEY_SPEAK_ON_CARD, true);
        switchSpeakCard.setChecked(speakOnCard);
        switchSpeakCard.setOnCheckedChangeListener((v, checked) ->
                prefs.edit().putBoolean(KEY_SPEAK_ON_CARD, checked).apply());

        updateLanguageLabels();

        btnExport.setOnClickListener(v -> exportToCsv());
        btnImport.setOnClickListener(v -> openFilePickerForImport());
        btnResetAllProgress.setOnClickListener(v -> confirmResetAllProgress());
        btnLanguage.setOnClickListener(v -> showTargetDialog());
        btnBaseLanguage.setOnClickListener(v -> showBaseDialog());
        btnAddStarters.setOnClickListener(v -> addStartersForCurrentLanguage());
    }

    private void updateLanguageLabels() {
        String base = LanguageManager.getBaseLang(this);
        LanguageManager.Language target = LanguageManager.getTarget(this);
        tvCurrentLanguage.setText(target.flag + "  " + target.displayName(base));
        LanguageManager.Language baseLang = LanguageManager.fromCode(base);
        tvCurrentBaseLanguage.setText(baseLang.flag + "  " + baseLang.displayName(base));
    }

    private void showTargetDialog() {
        String base = LanguageManager.getBaseLang(this);
        List<LanguageManager.Language> targets = LanguageManager.getAvailableTargets(base);
        String[] options = new String[targets.size()];
        int selected = -1;
        String currentCode = LanguageManager.getTargetCode(this);
        for (int i = 0; i < targets.size(); i++) {
            options[i] = targets.get(i).flag + "  " + targets.get(i).displayName(base);
            if (targets.get(i).code.equals(currentCode)) selected = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.change_language)
                .setSingleChoiceItems(options, selected, (d, which) -> {
                    LanguageManager.Language chosen = targets.get(which);
                    LanguageManager.setTarget(this, chosen.code);
                    updateLanguageLabels();
                    Toast.makeText(this, getString(R.string.language_changed,
                            chosen.displayName(base)), Toast.LENGTH_SHORT).show();
                    d.dismiss();
                    recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showBaseDialog() {
        String currentBase = LanguageManager.getBaseLang(this);
        String[] options = new String[LanguageManager.ALL_BASE.length];
        int selected = -1;
        for (int i = 0; i < LanguageManager.ALL_BASE.length; i++) {
            LanguageManager.Language l = LanguageManager.ALL_BASE[i];
            options[i] = l.flag + "  " + l.nameRu + " / " + l.nameEn;
            if (l.code.equals(currentBase)) selected = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.change_base_language)
                .setSingleChoiceItems(options, selected, (d, which) -> {
                    LanguageManager.Language chosen = LanguageManager.ALL_BASE[which];
                    d.dismiss();
                    if (chosen.code.equals(currentBase)) return;
                    confirmBaseChange(chosen);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmBaseChange(LanguageManager.Language newBase) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.recreate_starters_title)
                .setMessage(R.string.recreate_starters_msg)
                .setPositiveButton(R.string.recreate_starters_yes,
                        (d, w) -> applyBaseChange(newBase, true))
                .setNeutralButton(R.string.recreate_starters_no,
                        (d, w) -> applyBaseChange(newBase, false))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applyBaseChange(LanguageManager.Language newBase, boolean recreateStarters) {
        String currentTarget = LanguageManager.getTargetCode(this);
        if (newBase.code.equals(currentTarget)) {
            String fallback = "he".equals(newBase.code) ? "en" : "he";
            LanguageManager.setTarget(this, fallback);
        }
        LanguageManager.setBaseLang(this, newBase.code);

        if (recreateStarters) {
            executor.execute(() -> {
                recreateStarterFolders(newBase.code);
                runOnUiThread(() -> {
                    Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            });
        } else {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Удаляет все стартовые папки (по именам из StarterData) во всех target-языках
     * и создаёт их заново с переводами на новый base-язык.
     */
    private void recreateStarterFolders(String newBaseLang) {
        long now = System.currentTimeMillis();
        for (LanguageManager.Language target : LanguageManager.ALL_TARGETS) {
            if (target.code.equals(newBaseLang)) continue;

            // 1) Удаляем стартовые папки
            List<Folder> folders = db.folderDao().getAllFolders(target.code);
            for (Folder f : folders) {
                if (StarterData.isStarterFolderName(f.name, target.code)) {
                    db.wordDao().deleteWordsInFolder(f.id);
                    db.folderDao().deleteFolderById(f.id);
                }
            }

            // 2) Создаём заново
            List<StarterData.StarterFolder> starters = StarterData.getFor(target.code);
            for (StarterData.StarterFolder sf : starters) {
                Folder folder = new Folder(sf.name(newBaseLang), target.code);
                long folderId = db.folderDao().insert(folder);
                int order = 0;
                for (StarterData.StarterWord sw : sf.words) {
                    Word word = new Word(sw.target,
                            sw.nativeText(newBaseLang), sw.commentText(newBaseLang));
                    word.languageCode = target.code;
                    word.folderId = (int) folderId;
                    word.isSaved = true;
                    word.sortOrder = order++;
                    db.wordDao().insert(word);
                }
                db.folderDao().updateTimestamp((int) folderId, now);
            }
        }
    }

    private void addStartersForCurrentLanguage() {
        String base = LanguageManager.getBaseLang(this);
        LanguageManager.Language target = LanguageManager.getTarget(this);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_starters_q)
                .setMessage(getString(R.string.add_starters_msg, target.displayName(base)))
                .setPositiveButton(R.string.add, (d, w) -> executor.execute(() -> {
                    List<StarterData.StarterFolder> starters = StarterData.getFor(target.code);
                    int added = 0;
                    long now = System.currentTimeMillis();
                    for (StarterData.StarterFolder sf : starters) {
                        String folderName = sf.name(base);
                        List<Folder> existing = db.folderDao().getAllFolders(target.code);
                        boolean nameTaken = false;
                        for (Folder f : existing) {
                            if (f.name.equals(folderName)) { nameTaken = true; break; }
                        }
                        if (nameTaken) folderName = folderName + " (" + (now % 10000) + ")";

                        Folder folder = new Folder(folderName, target.code);
                        long folderId = db.folderDao().insert(folder);

                        int order = 0;
                        for (StarterData.StarterWord sw : sf.words) {
                            Word word = new Word(sw.target, sw.nativeText(base), sw.commentText(base));
                            word.languageCode = target.code;
                            word.folderId = (int) folderId;
                            word.isSaved = true;
                            word.sortOrder = order++;
                            db.wordDao().insert(word);
                            added++;
                        }
                        db.folderDao().updateTimestamp((int) folderId, now);
                    }
                    final int total = added;
                    runOnUiThread(() -> Toast.makeText(this,
                            getString(R.string.added_words_count, total), Toast.LENGTH_LONG).show());
                }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmResetAllProgress() {
        String base = LanguageManager.getBaseLang(this);
        LanguageManager.Language target = LanguageManager.getTarget(this);

        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_all_progress_q)
                .setMessage(getString(R.string.reset_all_progress_msg, target.displayName(base)))
                .setPositiveButton(R.string.reset, (d, w) -> executor.execute(() -> {
                    db.wordDao().resetAllProgressForLanguage(target.code);
                    runOnUiThread(() -> Toast.makeText(SettingsActivity.this,
                            R.string.progress_reset, Toast.LENGTH_SHORT).show());
                }))
                .setNegativeButton(R.string.cancel, null).show();
    }

    private void openFilePickerForImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/csv", "text/comma-separated-values", "text/plain", "application/csv"
        });
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) importFromCsv(uri);
        }
    }

    // ============================== EXPORT ==============================

    private void exportToCsv() {
        btnExport.setEnabled(false);
        btnImport.setEnabled(false);
        exportProgress.setVisibility(View.VISIBLE);
        exportStatus.setText(R.string.exporting);
        exportStatus.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                StringBuilder csv = new StringBuilder();
                csv.append("Lang,Folder,Native,Target,Gender,Comment,Learned\n");

                for (LanguageManager.Language lang : LanguageManager.ALL_TARGETS) {
                    List<Folder> folders = db.folderDao().getAllFoldersNewestFirst(lang.code);
                    Set<Integer> exported = new HashSet<>();

                    for (Folder folder : folders) {
                        List<Word> words = db.wordDao().getWordsInFolder(lang.code, folder.id);
                        for (Word word : words) {
                            csv.append(escapeCsv(lang.code)).append(",")
                                    .append(escapeCsv(folder.name)).append(",")
                                    .append(escapeCsv(word.russian)).append(",")
                                    .append(escapeCsv(word.hebrew)).append(",")
                                    .append(escapeCsv(word.gender != null ? word.gender : "")).append(",")
                                    .append(escapeCsv(word.comment != null ? word.comment : "")).append(",")
                                    .append(word.isLearned ? "1" : "0").append("\n");
                            exported.add(word.id);
                        }
                    }
                    for (Word word : db.wordDao().getAllSaved(lang.code)) {
                        if (exported.contains(word.id)) continue;
                        Folder folder = db.folderDao().getFolderById(word.folderId);
                        String folderName = folder != null ? folder.name : "Orphan";
                        csv.append(escapeCsv(lang.code)).append(",")
                                .append(escapeCsv(folderName)).append(",")
                                .append(escapeCsv(word.russian)).append(",")
                                .append(escapeCsv(word.hebrew)).append(",")
                                .append(escapeCsv(word.gender != null ? word.gender : "")).append(",")
                                .append(escapeCsv(word.comment != null ? word.comment : "")).append(",")
                                .append(word.isLearned ? "1" : "0").append("\n");
                    }
                }

                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.getDefault())
                        .format(new Date());
                String fileName = "moy_ivrit_" + timestamp + ".csv";
                String savedLocation;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                    values.put(MediaStore.Downloads.IS_PENDING, 1);
                    Uri uri = getContentResolver().insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) throw new Exception("Could not create file");
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        os.write(0xEF); os.write(0xBB); os.write(0xBF);
                        os.write(csv.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    values.clear();
                    values.put(MediaStore.Downloads.IS_PENDING, 0);
                    getContentResolver().update(uri, values, null, null);
                    savedLocation = "Downloads/" + fileName;
                } else {
                    File downloads = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);
                    File file = new File(downloads, fileName);
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(0xEF); fos.write(0xBB); fos.write(0xBF);
                        fos.write(csv.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    savedLocation = file.getAbsolutePath();
                }

                final String finalLocation = savedLocation;
                runOnUiThread(() -> {
                    exportProgress.setVisibility(View.GONE);
                    exportStatus.setText(getString(R.string.file_saved, finalLocation));
                    btnExport.setEnabled(true);
                    btnImport.setEnabled(true);
                });

            } catch (Exception e) {
                Log.e(TAG, "Export error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    exportProgress.setVisibility(View.GONE);
                    exportStatus.setText(e.getMessage());
                    btnExport.setEnabled(true);
                    btnImport.setEnabled(true);
                });
            }
        });
    }

    // ============================== IMPORT ==============================

    private void importFromCsv(Uri uri) {
        btnExport.setEnabled(false);
        btnImport.setEnabled(false);
        exportProgress.setVisibility(View.VISIBLE);
        exportStatus.setText(R.string.importing);
        exportStatus.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            int importedCount = 0;
            int importedFolders = 0;
            try (InputStream is = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(is, StandardCharsets.UTF_8))) {

                String header = reader.readLine();
                if (header == null) throw new Exception("Empty file");
                if (header.startsWith("\uFEFF")) header = header.substring(1);

                boolean newFormat = header.toLowerCase().startsWith("lang");
                boolean ruOldFormat = header.toLowerCase().startsWith("язык");

                Set<String> usedNames = new HashSet<>();
                for (LanguageManager.Language lang : LanguageManager.ALL_TARGETS) {
                    for (Folder f : db.folderDao().getAllFolders(lang.code))
                        usedNames.add(f.name);
                }

                Map<String, Integer> folderMap = new HashMap<>();
                Map<Integer, Integer> folderMaxOrder = new HashMap<>();

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    List<String> fields = parseCsvLine(line);

                    String langCode, folderName, nativeText, target, gender, comment;
                    boolean isLearned;

                    if ((newFormat || ruOldFormat) && fields.size() >= 7) {
                        langCode = fields.get(0).trim();
                        folderName = fields.get(1).trim();
                        nativeText = fields.get(2).trim();
                        target = fields.get(3).trim();
                        gender = fields.get(4).trim();
                        comment = fields.get(5).trim();
                        isLearned = fields.get(6).trim().equals("1") ||
                                fields.get(6).trim().equalsIgnoreCase("Да") ||
                                fields.get(6).trim().equalsIgnoreCase("Yes");
                    } else if (fields.size() >= 6) {
                        langCode = "he";
                        folderName = fields.get(0).trim();
                        nativeText = fields.get(1).trim();
                        target = fields.get(2).trim();
                        gender = fields.get(3).trim();
                        comment = fields.get(4).trim();
                        isLearned = fields.get(5).trim().equalsIgnoreCase("Да") ||
                                fields.get(5).trim().equalsIgnoreCase("Yes");
                    } else continue;

                    if (langCode.isEmpty()) langCode = "he";
                    if (nativeText.isEmpty() || target.isEmpty()) continue;

                    String mapKey = langCode + "|" + folderName;
                    int folderId;
                    if (folderMap.containsKey(mapKey)) {
                        folderId = folderMap.get(mapKey);
                    } else {
                        String baseName = folderName.isEmpty() ? "Import" : folderName;
                        String unique = baseName;
                        int counter = 1;
                        while (usedNames.contains(unique)) {
                            unique = baseName + " (" + counter + ")";
                            counter++;
                        }
                        Folder newFolder = new Folder(unique, langCode);
                        long newId = db.folderDao().insert(newFolder);
                        folderId = (int) newId;
                        usedNames.add(unique);
                        folderMap.put(mapKey, folderId);
                        importedFolders++;
                    }

                    int nextOrder;
                    if (folderMaxOrder.containsKey(folderId)) {
                        nextOrder = folderMaxOrder.get(folderId) + 1;
                    } else {
                        nextOrder = db.wordDao().getMaxSortOrderInFolder(folderId) + 1;
                    }
                    folderMaxOrder.put(folderId, nextOrder);

                    Word word = new Word(target, nativeText, comment.isEmpty() ? null : comment);
                    word.id = 0;
                    word.isSaved = true;
                    word.gender = gender.isEmpty() ? null : gender;
                    word.folderId = folderId;
                    word.isLearned = isLearned;
                    word.sortOrder = nextOrder;
                    word.languageCode = langCode;
                    db.wordDao().insert(word);
                    importedCount++;
                }

                long now = System.currentTimeMillis();
                for (Integer fid : folderMap.values()) {
                    db.folderDao().updateTimestamp(fid, now);
                }

                final int fc = importedCount;
                runOnUiThread(() -> {
                    exportProgress.setVisibility(View.GONE);
                    if (fc == 0) {
                        exportStatus.setText(R.string.import_empty);
                    } else {
                        exportStatus.setText(getString(R.string.import_done, fc));
                    }
                    btnExport.setEnabled(true);
                    btnImport.setEnabled(true);
                });

            } catch (Exception e) {
                Log.e(TAG, "Import error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    exportProgress.setVisibility(View.GONE);
                    exportStatus.setText(e.getMessage());
                    btnExport.setEnabled(true);
                    btnImport.setEnabled(true);
                });
            }
        });
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (q && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"'); i++;
                } else q = !q;
            } else if (c == ',' && !q) {
                result.add(sb.toString()); sb.setLength(0);
            } else sb.append(c);
        }
        result.add(sb.toString());
        return result;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}