package com.example.advancedrussian_hebrewdict.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface WordDao {

    @Insert
    long insert(Word word);

    @Update
    void update(Word word);

    @Update
    void updateAll(List<Word> words);

    @Query("SELECT * FROM words WHERE isSaved = 1 AND languageCode = :lang ORDER BY id DESC")
    List<Word> getAllSaved(String lang);

    @Query("SELECT * FROM words WHERE isSaved = 1 AND languageCode = :lang " +
            "AND folderId = :folderId ORDER BY sortOrder ASC, id ASC")
    List<Word> getWordsInFolder(String lang, int folderId);

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM words WHERE folderId = :folderId")
    int getMaxSortOrderInFolder(int folderId);

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    Word getWordById(int id);

    @Query("DELETE FROM words WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM words WHERE isSaved = 0")
    void deleteAllNonSaved();

    @Query("DELETE FROM words WHERE folderId = :folderId")
    void deleteWordsInFolder(int folderId);

    @Query("UPDATE words SET correctStreak = 0, isLearned = 0 WHERE folderId = :folderId")
    void resetProgressInFolder(int folderId);

    @Query("UPDATE words SET correctStreak = 0, isLearned = 0 WHERE isSaved = 1 AND languageCode = :lang")
    void resetAllProgressForLanguage(String lang);
}