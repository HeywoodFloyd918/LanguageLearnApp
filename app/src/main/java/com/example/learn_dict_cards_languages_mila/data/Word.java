package com.example.learn_dict_cards_languages_mila.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "words")
public class Word {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String hebrew;
    public String russian;
    public String comment;
    public boolean isSaved;
    public String gender;
    public int folderId;
    public int correctStreak;
    public boolean isLearned;
    public int sortOrder;

    @NonNull
    @ColumnInfo(defaultValue = "he")
    public String languageCode = "he";

    // Временное поле для хранения вероятности перевода (в БД не сохраняется)
    @Ignore
    public float probability = 0f;

    public Word(String hebrew, String russian, String comment) {
        this.hebrew = hebrew;
        this.russian = russian;
        this.comment = comment;
        this.isSaved = false;
        this.gender = null;
        this.folderId = 0;
        this.correctStreak = 0;
        this.isLearned = false;
        this.sortOrder = 0;
        this.languageCode = "he";
    }
}