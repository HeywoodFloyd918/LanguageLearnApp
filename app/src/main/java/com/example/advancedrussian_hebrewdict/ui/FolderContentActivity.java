package com.example.advancedrussian_hebrewdict.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.advancedrussian_hebrewdict.R;
import com.example.advancedrussian_hebrewdict.data.AppDatabase;
import com.example.advancedrussian_hebrewdict.data.Word;
import com.example.advancedrussian_hebrewdict.ui.adapter.WordAdapter;
import com.example.advancedrussian_hebrewdict.util.LanguageManager;
import com.example.advancedrussian_hebrewdict.util.SpeechHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FolderContentActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private WordAdapter adapter;
    private AppDatabase db;
    private TextView emptyText;
    private int folderId;
    private String folderName;
    private SpeechHelper speechHelper;
    private String targetCode;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_content);

        folderId = getIntent().getIntExtra("folder_id", -1);
        folderName = getIntent().getStringExtra("folder_name");
        if (folderId == -1 || folderName == null) { finish(); return; }

        targetCode = LanguageManager.getTargetCode(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(folderName);
        }

        db = AppDatabase.getInstance(this);
        speechHelper = new SpeechHelper(this);
        emptyText = findViewById(R.id.folder_empty);
        emptyText.setText(R.string.folder_empty);
        recyclerView = findViewById(R.id.recycler_view_folder);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WordAdapter(new ArrayList<>(),
                word -> {
                    Intent intent = new Intent(FolderContentActivity.this, DetailActivity.class);
                    intent.putExtra("word_id", word.id);
                    startActivity(intent);
                },
                word -> {
                    if (word.hebrew != null && !word.hebrew.isEmpty()) {
                        speechHelper.speakAndPlay(word.hebrew, null);
                    }
                });
        recyclerView.setAdapter(adapter);

        setupDragAndDrop();
        loadWords();

        FloatingActionButton fabAdd = findViewById(R.id.fab_add_word);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(FolderContentActivity.this, EditWordActivity.class);
            intent.putExtra("default_folder_id", folderId);
            startActivity(intent);
        });
    }

    private void setupDragAndDrop() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                adapter.onItemMove(viewHolder.getAdapterPosition(),
                        target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

            @Override
            public boolean isLongPressDragEnabled() { return true; }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                saveNewOrder();
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void saveNewOrder() {
        List<Word> words = adapter.getWords();
        for (int i = 0; i < words.size(); i++) {
            words.get(i).sortOrder = i;
        }
        final int fid = folderId;
        executor.execute(() -> {
            db.wordDao().updateAll(words);
            db.folderDao().updateTimestamp(fid, System.currentTimeMillis());
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWords();
    }

    private void loadWords() {
        executor.execute(() -> {
            List<Word> words = db.wordDao().getWordsInFolder(targetCode, folderId);
            runOnUiThread(() -> {
                adapter.updateList(words);
                emptyText.setVisibility(words.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechHelper != null) speechHelper.close();
        executor.shutdown();
    }
}