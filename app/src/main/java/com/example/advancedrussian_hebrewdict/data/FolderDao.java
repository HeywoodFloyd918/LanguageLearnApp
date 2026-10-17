package com.example.advancedrussian_hebrewdict.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface FolderDao {

    @Insert
    long insert(Folder folder);

    @Update
    void update(Folder folder);

    @Query("SELECT * FROM folders WHERE languageCode = :lang ORDER BY createdAt ASC")
    List<Folder> getAllFolders(String lang);

    @Query("SELECT * FROM folders WHERE languageCode = :lang ORDER BY createdAt DESC")
    List<Folder> getAllFoldersNewestFirst(String lang);

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    Folder getFolderById(int id);

    @Query("SELECT * FROM folders WHERE languageCode = :lang ORDER BY createdAt DESC LIMIT 1")
    Folder getLastCreatedFolder(String lang);

    @Query("SELECT COUNT(*) FROM folders WHERE languageCode = :lang")
    int getFolderCount(String lang);

    @Query("SELECT COUNT(*) FROM folders")
    int getTotalFolderCount();

    @Query("DELETE FROM folders WHERE id = :id")
    void deleteFolderById(int id);

    /** Обновляет дату последнего изменения содержимого папки. */
    @Query("UPDATE folders SET updatedAt = :timestamp WHERE id = :folderId")
    void updateTimestamp(int folderId, long timestamp);

    @Query("SELECT f.id AS id, f.name AS name, f.createdAt AS createdAt, " +
            "f.updatedAt AS updatedAt, " +
            "COUNT(w.id) AS wordCount " +
            "FROM folders f " +
            "LEFT JOIN words w ON w.folderId = f.id AND w.isSaved = 1 " +
            "WHERE f.languageCode = :lang " +
            "GROUP BY f.id, f.name, f.createdAt, f.updatedAt " +
            "ORDER BY f.createdAt DESC")
    List<FolderWithCount> getFoldersWithCount(String lang);
}