package com.example.learn_dict_cards_languages_mila.ui.training;

import android.content.Context;

import com.example.learn_dict_cards_languages_mila.R;

/**
 * Режимы тренировки. Отображаемые имена берутся из strings.xml,
 * поэтому корректно локализуются.
 */
public enum TrainingMode {

    CHOICE(0, false, false, R.string.mode_choice),
    WRITE_TARGET(1, true, false, R.string.mode_write_target),
    WRITE_NATIVE(2, true, false, R.string.mode_write_native),
    WRITE_BOTH(3, true, false, R.string.mode_write_both),
    FILL_BLANK_CHOICE(4, false, true, R.string.mode_fill_blank_choice),
    FILL_BLANK_WRITE(5, true, true, R.string.mode_fill_blank_write);

    private final int index;
    private final boolean writeMode;
    private final boolean fillBlankMode;
    private final int nameResId;

    TrainingMode(int index, boolean writeMode, boolean fillBlankMode, int nameResId) {
        this.index = index;
        this.writeMode = writeMode;
        this.fillBlankMode = fillBlankMode;
        this.nameResId = nameResId;
    }

    public int getIndex() { return index; }
    public boolean isWriteMode() { return writeMode; }
    public boolean isFillBlankMode() { return fillBlankMode; }
    public boolean isChoiceMode() { return !writeMode; }
    public boolean needsSentence() { return fillBlankMode; }

    public static TrainingMode fromIndex(int index) {
        for (TrainingMode m : values()) {
            if (m.index == index) return m;
        }
        return CHOICE;
    }

    public static String[] displayNames(Context ctx) {
        TrainingMode[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = ctx.getString(values[i].nameResId);
        }
        return names;
    }
}