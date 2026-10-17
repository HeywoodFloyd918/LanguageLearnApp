package com.example.advancedrussian_hebrewdict;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.advancedrussian_hebrewdict.data.AppDatabase;
import com.example.advancedrussian_hebrewdict.data.Folder;
import com.example.advancedrussian_hebrewdict.data.FolderWithCount;
import com.example.advancedrussian_hebrewdict.data.StarterData;
import com.example.advancedrussian_hebrewdict.data.Word;
import com.example.advancedrussian_hebrewdict.ui.BaseActivity;
import com.example.advancedrussian_hebrewdict.ui.EditWordActivity;
import com.example.advancedrussian_hebrewdict.ui.FolderContentActivity;
import com.example.advancedrussian_hebrewdict.ui.GrammarActivity;
import com.example.advancedrussian_hebrewdict.ui.LanguagePickerDialog;
import com.example.advancedrussian_hebrewdict.ui.SearchActivity;
import com.example.advancedrussian_hebrewdict.ui.SettingsActivity;
import com.example.advancedrussian_hebrewdict.ui.adapter.FolderAdapter;
import com.example.advancedrussian_hebrewdict.ui.training.TrainingActivity;
import com.example.advancedrussian_hebrewdict.util.LanguageManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_FIRST_LAUNCH_DONE = "first_launch_done";

    private RecyclerView recyclerView;
    private FolderAdapter adapter;
    private AppDatabase db;
    private DrawerLayout drawerLayout;
    private TextView titleView;
    private TextView navGrammar;
    private LanguageManager.Language targetLang;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        Button btnCreateFolder = findViewById(R.id.btn_create_folder);
        titleView = findViewById(R.id.title);
        TextView navSettings = findViewById(R.id.nav_item_settings);
        TextView navAddWord = findViewById(R.id.nav_item_add_word);
        TextView navTraining = findViewById(R.id.nav_item_training);
        navGrammar = findViewById(R.id.nav_item_grammar);

        db = AppDatabase.getInstance(this);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FolderAdapter(
                new ArrayList<>(),
                folder -> {
                    Intent intent = new Intent(MainActivity.this, FolderContentActivity.class);
                    intent.putExtra("folder_id", folder.id);
                    intent.putExtra("folder_name", folder.name);
                    startActivity(intent);
                },
                this::showFolderOptions
        );
        recyclerView.setAdapter(adapter);

        btnCreateFolder.setOnClickListener(v -> showCreateFolderDialog());

        navSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
        navAddWord.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(MainActivity.this, EditWordActivity.class));
        });
        navTraining.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(MainActivity.this, TrainingActivity.class));
        });
        navGrammar.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(MainActivity.this, GrammarActivity.class));
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        executor.execute(() -> db.wordDao().deleteAllNonSaved());

        if (!LanguageManager.isPicked(this)) {
            executor.execute(() -> {
                int totalFolders = db.folderDao().getTotalFolderCount();
                runOnUiThread(() -> {
                    if (totalFolders > 0) {
                        LanguageManager.setBaseLang(MainActivity.this, LanguageManager.BASE_RU);
                        LanguageManager.setTarget(MainActivity.this, "he");
                        targetLang = LanguageManager.HEBREW;
                        afterLanguageReady();
                    } else {
                        LanguagePickerDialog.show(MainActivity.this, (base, target) -> {
                            LanguageManager.setBaseLang(MainActivity.this, base);
                            LanguageManager.setTarget(MainActivity.this, target);
                            targetLang = LanguageManager.fromCode(target);
                            executor.execute(() -> {
                                if (db.folderDao().getFolderCount(target) == 0) {
                                    addStartersIfNeeded(target, base);
                                }
                            });
                            recreate();
                        });
                    }
                });
            });
        } else {
            targetLang = LanguageManager.getTarget(this);
            afterLanguageReady();
        }
    }

    private void afterLanguageReady() {
        runOnUiThread(() -> {
            updateTitle();
            updateGrammarMenuTitle();
            loadFolders();
        });
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = !prefs.getBoolean(KEY_FIRST_LAUNCH_DONE, false);
        if (isFirstLaunch) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply();
            showDevWarningDialog();
        }
    }

    private void updateTitle() {
        if (titleView != null && targetLang != null) {
            titleView.setText(getString(R.string.my_folders) + " " + targetLang.flag);
        }
    }

    private void updateGrammarMenuTitle() {
        if (navGrammar == null || targetLang == null) return;
        switch (targetLang.code) {
            case "en": navGrammar.setText(R.string.grammar_title_en); break;
            case "es": navGrammar.setText(R.string.grammar_title_es); break;
            default:   navGrammar.setText(R.string.grammar_title_he); break;
        }
    }

    private void addStartersIfNeeded(String targetCode, String baseLang) {
        if (db.folderDao().getFolderCount(targetCode) > 0) return;
        List<StarterData.StarterFolder> starters = StarterData.getFor(targetCode);
        long now = System.currentTimeMillis();
        for (StarterData.StarterFolder sf : starters) {
            Folder folder = new Folder(sf.name(baseLang), targetCode);
            long folderId = db.folderDao().insert(folder);
            int order = 0;
            for (StarterData.StarterWord sw : sf.words) {
                Word word = new Word(sw.target, sw.nativeText(baseLang), sw.commentText(baseLang));
                word.languageCode = targetCode;
                word.folderId = (int) folderId;
                word.isSaved = true;
                word.sortOrder = order++;
                db.wordDao().insert(word);
            }
            db.folderDao().updateTimestamp((int) folderId, now);
        }
    }

    private void showDevWarningDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_dev_warning, null);
        new AlertDialog.Builder(this).setView(view)
                .setPositiveButton(R.string.got_it, null).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LanguageManager.Language lang = LanguageManager.getTarget(this);
        if (targetLang == null || !targetLang.code.equals(lang.code)) {
            targetLang = lang;
            updateTitle();
            updateGrammarMenuTitle();
        }

        final String target = targetLang != null ? targetLang.code : "he";
        final String base = LanguageManager.getBaseLang(this);
        executor.execute(() -> {
            if (db.folderDao().getFolderCount(target) == 0) {
                addStartersIfNeeded(target, base);
            }
            List<FolderWithCount> folders = db.folderDao().getFoldersWithCount(target);
            runOnUiThread(() -> adapter.updateList(folders));
        });
    }

    private void loadFolders() {
        final String target = targetLang != null ? targetLang.code : "he";
        executor.execute(() -> {
            List<FolderWithCount> folders = db.folderDao().getFoldersWithCount(target);
            runOnUiThread(() -> adapter.updateList(folders));
        });
    }

    private void showCreateFolderDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        final EditText input = new EditText(this);
        input.setHint(R.string.folder_name_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        container.addView(input);

        TextView mergeLink = new TextView(this);
        mergeLink.setText(R.string.merge_folders_link);
        mergeLink.setTextSize(12);
        mergeLink.setTextColor(0xFF1976D2);
        mergeLink.setGravity(Gravity.END);
        mergeLink.setPadding(8, pad / 2, 8, 8);
        mergeLink.setClickable(true);
        mergeLink.setFocusable(true);
        mergeLink.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(mergeLink);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.create_folder);
        builder.setView(container);
        builder.setPositiveButton(R.string.create, (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                executor.execute(() -> {
                    Folder f = new Folder(name, targetLang.code);
                    db.folderDao().insert(f);
                    loadFolders();
                });
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();
        mergeLink.setOnClickListener(v -> {
            dialog.dismiss();
            openMergeFoldersDialog();
        });
        dialog.show();
    }

    private void openMergeFoldersDialog() {
        final String target = targetLang.code;
        executor.execute(() -> {
            List<Folder> folders = db.folderDao().getAllFolders(target);
            runOnUiThread(() -> {
                if (folders.size() < 2) {
                    Toast.makeText(this, R.string.need_min_two_folders, Toast.LENGTH_SHORT).show();
                    return;
                }
                showMergeFoldersDialogUi(folders);
            });
        });
    }

    private void showMergeFoldersDialogUi(List<Folder> folders) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, pad / 2);

        final EditText input = new EditText(this);
        input.setHint(R.string.merge_folders_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        container.addView(input);

        TextView label = new TextView(this);
        label.setText(R.string.choose_folders_for_merge);
        label.setTextSize(13);
        label.setTextColor(0xFF757575);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = pad / 2;
        label.setLayoutParams(lp);
        container.addView(label);

        final List<CheckBox> boxes = new ArrayList<>();
        for (Folder f : folders) {
            CheckBox cb = new CheckBox(this);
            cb.setText(f.name);
            container.addView(cb);
            boxes.add(cb);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(container);

        new AlertDialog.Builder(this)
                .setTitle(R.string.merge_folders_title)
                .setView(scroll)
                .setPositiveButton(R.string.merge, (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.enter_name, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Set<Integer> ids = new HashSet<>();
                    for (int i = 0; i < boxes.size(); i++) {
                        if (boxes.get(i).isChecked()) ids.add(folders.get(i).id);
                    }
                    if (ids.size() < 2) {
                        Toast.makeText(this, R.string.choose_min_two_folders, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    performMerge(ids, name);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performMerge(Set<Integer> folderIds, String newName) {
        executor.execute(() -> {
            Folder newFolder = new Folder(newName, targetLang.code);
            long newId = db.folderDao().insert(newFolder);
            int newFolderId = (int) newId;

            int sortOrder = 0;
            for (Integer oldId : folderIds) {
                List<Word> words = db.wordDao().getWordsInFolder(targetLang.code, oldId);
                for (Word word : words) {
                    word.folderId = newFolderId;
                    word.sortOrder = sortOrder++;
                    db.wordDao().update(word);
                }
            }
            for (Integer oldId : folderIds) db.folderDao().deleteFolderById(oldId);

            runOnUiThread(() -> {
                loadFolders();
                Toast.makeText(MainActivity.this,
                        getString(R.string.folders_merged, newName), Toast.LENGTH_LONG).show();
            });
        });
    }

    private void showFolderOptions(FolderWithCount folder) {
        String[] options = {
                getString(R.string.rename),
                getString(R.string.delete),
                getString(R.string.reset_progress_in_folder)
        };
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.folder_label, folder.name))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showRenameFolderDialog(folder);
                    else if (which == 1) confirmDeleteFolder(folder);
                    else confirmResetProgress(folder);
                })
                .show();
    }

    private void showRenameFolderDialog(FolderWithCount folder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.rename_folder);
        final EditText input = new EditText(this);
        input.setText(folder.name);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSelection(input.getText().length());
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = padding;
        params.rightMargin = padding;
        params.topMargin = padding / 2;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                executor.execute(() -> {
                    Folder f = db.folderDao().getFolderById(folder.id);
                    if (f != null) {
                        f.name = newName;
                        f.updatedAt = System.currentTimeMillis();
                        db.folderDao().update(f);
                    }
                    loadFolders();
                });
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void confirmDeleteFolder(FolderWithCount folder) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_folder_q)
                .setMessage(getString(R.string.delete_folder_msg, folder.name))
                .setPositiveButton(R.string.delete, (d, w) -> executor.execute(() -> {
                    db.wordDao().deleteWordsInFolder(folder.id);
                    db.folderDao().deleteFolderById(folder.id);
                    loadFolders();
                }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmResetProgress(FolderWithCount folder) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_progress_q)
                .setMessage(getString(R.string.reset_progress_msg, folder.name))
                .setPositiveButton(R.string.reset, (d, w) -> executor.execute(() -> {
                    db.wordDao().resetProgressInFolder(folder.id);
                    db.folderDao().updateTimestamp(folder.id, System.currentTimeMillis());
                    loadFolders();
                }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null && searchItem.getActionView() != null) {
            searchItem.getActionView().setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, SearchActivity.class)));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        }
        if (id == R.id.action_menu) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}