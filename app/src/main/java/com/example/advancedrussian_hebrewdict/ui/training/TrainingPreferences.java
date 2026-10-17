package com.example.advancedrussian_hebrewdict.ui.training;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Обёртка над SharedPreferences для настроек тренировки.
 * Хранит: последний режим, последний фильтр, выбранные папки, флаг "все папки".
 */
public class TrainingPreferences {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_LAST_MODE = "last_mode_index";
    private static final String KEY_LAST_FILTER = "last_filter_index";
    private static final String KEY_LAST_FOLDERS = "last_folder_ids";
    private static final String KEY_LAST_SELECT_ALL = "last_select_all";

    private final SharedPreferences prefs;

    public TrainingPreferences(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getLastModeIndex() {
        return prefs.getInt(KEY_LAST_MODE, 0);
    }

    public int getLastFilterIndex() {
        return prefs.getInt(KEY_LAST_FILTER, 0);
    }

    public boolean isLastSelectAll() {
        return prefs.getBoolean(KEY_LAST_SELECT_ALL, true);
    }

    public Set<Integer> getLastFolderIds() {
        Set<String> raw = prefs.getStringSet(KEY_LAST_FOLDERS, null);
        Set<Integer> result = new HashSet<>();
        if (raw == null) return result;
        for (String s : raw) {
            try {
                result.add(Integer.parseInt(s));
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    public void saveTrainingSettings(int modeIndex, int filterIndex,
                                     boolean selectAll, Set<Integer> folderIds) {
        Set<String> ids = new HashSet<>();
        for (Integer id : folderIds) ids.add(String.valueOf(id));

        prefs.edit()
                .putInt(KEY_LAST_MODE, modeIndex)
                .putInt(KEY_LAST_FILTER, filterIndex)
                .putBoolean(KEY_LAST_SELECT_ALL, selectAll)
                .putStringSet(KEY_LAST_FOLDERS, ids)
                .apply();
    }
}