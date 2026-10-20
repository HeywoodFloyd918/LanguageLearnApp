package com.example.learn_dict_cards_languages_mila.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Word.class, Folder.class}, version = 10, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract WordDao wordDao();
    public abstract FolderDao folderDao();

    private static AppDatabase instance;

    // Добавляет languageCode (когда-то было переходом 5→6)
    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            try {
                db.execSQL("ALTER TABLE words ADD COLUMN languageCode TEXT NOT NULL DEFAULT 'he'");
            } catch (Exception ignored) { }
            try {
                db.execSQL("ALTER TABLE folders ADD COLUMN languageCode TEXT NOT NULL DEFAULT 'he'");
            } catch (Exception ignored) { }
        }
    };

    // Добавляет updatedAt (когда-то было переходом 6→7)
    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            try {
                db.execSQL("ALTER TABLE folders ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0");
            } catch (Exception ignored) { }
            db.execSQL("UPDATE folders SET updatedAt = createdAt WHERE updatedAt = 0");
        }
    };

    // Фиктивные пустые миграции — чтобы покрыть любой возможный путь
    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) { }
    };
    private static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) { }
    };
    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) { }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "word_database")
                    .addMigrations(
                            MIGRATION_5_6,
                            MIGRATION_6_7,
                            MIGRATION_7_8,
                            MIGRATION_8_9,
                            MIGRATION_9_10
                    )
                    .build();  // ВАЖНО: fallbackToDestructiveMigration() убран!
        }
        return instance;
    }
}