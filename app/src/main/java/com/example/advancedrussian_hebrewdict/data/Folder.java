package com.example.advancedrussian_hebrewdict.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "folders")
public class Folder {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public long createdAt;
    public long updatedAt;

    @NonNull
    @ColumnInfo(defaultValue = "he")
    public String languageCode = "he";

    public Folder(String name, String languageCode) {
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.languageCode = languageCode;
    }

    @Ignore
    public Folder(String name) {
        this(name, "he");
    }
}