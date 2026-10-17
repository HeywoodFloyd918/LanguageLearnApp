package com.example.advancedrussian_hebrewdict.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class LanguageManager {

    private static final String PREFS = "app_settings";
    private static final String KEY_BASE_LANG = "base_language";
    private static final String KEY_TARGET_LANG = "current_language";
    private static final String KEY_PICKED = "language_picked";

    public static final String BASE_RU = "ru";
    public static final String BASE_EN = "en";

    public static class Language {
        public final String code;
        public final String nameRu;
        public final String nameEn;
        public final String flag;
        public final String speechLocale;
        public final String speechVoice;

        public Language(String code, String nameRu, String nameEn, String flag,
                        String speechLocale, String speechVoice) {
            this.code = code;
            this.nameRu = nameRu;
            this.nameEn = nameEn;
            this.flag = flag;
            this.speechLocale = speechLocale;
            this.speechVoice = speechVoice;
        }

        public String displayName(String baseLang) {
            return BASE_EN.equals(baseLang) ? nameEn : nameRu;
        }
    }

    public static final Language HEBREW = new Language(
            "he", "Иврит", "Hebrew", "🇮🇱", "he-IL", "he-IL-AvriNeural");
    public static final Language ENGLISH = new Language(
            "en", "Английский", "English", "🇬🇧", "en-US", "en-US-JennyNeural");
    public static final Language SPANISH = new Language(
            "es", "Испанский", "Spanish", "🇪🇸", "es-ES", "es-ES-ElviraNeural");
    public static final Language RUSSIAN = new Language(
            "ru", "Русский", "Russian", "🇷🇺", "ru-RU", "ru-RU-SvetlanaNeural");

    public static final Language[] ALL_TARGETS = { HEBREW, ENGLISH, SPANISH };
    public static final Language[] ALL_BASE = { RUSSIAN, ENGLISH };

    public static Language fromCode(String code) {
        if (code == null) return HEBREW;
        switch (code) {
            case "en": return ENGLISH;
            case "es": return SPANISH;
            case "ru": return RUSSIAN;
            case "he": return HEBREW;
            default: return HEBREW;
        }
    }

    // ---- Base language (native) ----

    public static String getBaseLang(Context ctx) {
        if (ctx == null) return BASE_RU;
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return p.getString(KEY_BASE_LANG, BASE_RU);
    }

    public static void setBaseLang(Context ctx, String code) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putString(KEY_BASE_LANG, code).apply();
    }

    // ---- Target language (learning) ----

    public static String getTargetCode(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return p.getString(KEY_TARGET_LANG, "he");
    }

    public static Language getTarget(Context ctx) {
        return fromCode(getTargetCode(ctx));
    }

    public static void setTarget(Context ctx, String code) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit()
                .putString(KEY_TARGET_LANG, code)
                .putBoolean(KEY_PICKED, true)
                .apply();
    }

    public static boolean isPicked(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return p.getBoolean(KEY_PICKED, false);
    }

    public static void setPicked(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putBoolean(KEY_PICKED, true).apply();
    }

    // ---- Available targets based on base language ----

    public static List<Language> getAvailableTargets(String baseLang) {
        List<Language> result = new ArrayList<>();
        for (Language l : ALL_TARGETS) {
            if (!l.code.equals(baseLang)) result.add(l);
        }
        return result;
    }
}