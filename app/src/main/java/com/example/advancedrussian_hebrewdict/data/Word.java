package com.example.advancedrussian_hebrewdict.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
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

    /**
     * Имя параметра hebrew совпадает с именем поля hebrew — это обязательно для Room.
     * Семантически это «изучаемое слово» (иврит / английский / испанский).
     */
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