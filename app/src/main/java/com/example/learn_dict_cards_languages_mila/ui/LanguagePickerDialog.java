package com.example.learn_dict_cards_languages_mila.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.learn_dict_cards_languages_mila.R;
import com.example.learn_dict_cards_languages_mila.util.LanguageManager;

import java.util.List;

/**
 * Двухшаговый диалог выбора языка.
 * Шаг 1: родной язык (RU/EN).
 * Шаг 2: изучаемый язык — список зависит от родного (нельзя учить родной).
 */
public class LanguagePickerDialog {

    public interface OnPicked {
        void onPicked(String baseLang, String targetLang);
    }

    public static void show(Activity activity, OnPicked callback) {
        showBaseStep(activity, baseLang -> {
            showTargetStep(activity, baseLang, targetLang ->
                    callback.onPicked(baseLang, targetLang));
        });
    }

    // ===================== ШАГ 1: РОДНОЙ ЯЗЫК =====================

    private static void showBaseStep(Activity activity, OnBasePicked onPicked) {
        View view = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_language_picker_base, null);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(false)
                .create();

        view.findViewById(R.id.btn_base_ru).setOnClickListener(v -> {
            dialog.dismiss();
            onPicked.onPicked(LanguageManager.BASE_RU);
        });
        view.findViewById(R.id.btn_base_en).setOnClickListener(v -> {
            dialog.dismiss();
            onPicked.onPicked(LanguageManager.BASE_EN);
        });

        dialog.show();
    }

    // ===================== ШАГ 2: ИЗУЧАЕМЫЙ ЯЗЫК =====================

    private static void showTargetStep(Activity activity, String baseLang,
                                       OnTargetPicked onPicked) {
        View view = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_language_picker_target, null);

        TextView title = view.findViewById(R.id.picker_title);
        title.setText(LanguageManager.BASE_EN.equals(baseLang)
                ? "What language do you want to learn?"
                : "Какой язык вы хотите изучать?");

        LinearLayout container = view.findViewById(R.id.targets_container);
        List<LanguageManager.Language> targets = LanguageManager.getAvailableTargets(baseLang);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(false)
                .create();

        for (LanguageManager.Language lang : targets) {
            View item = LayoutInflater.from(activity)
                    .inflate(R.layout.item_language_option, container, false);

            TextView flag = item.findViewById(R.id.option_flag);
            TextView name = item.findViewById(R.id.option_name);

            flag.setText(lang.flag);
            name.setText(lang.displayName(baseLang));

            item.setOnClickListener(v -> {
                dialog.dismiss();
                onPicked.onPicked(lang.code);
            });

            container.addView(item);
        }

        dialog.show();
    }

    private interface OnBasePicked {
        void onPicked(String baseLang);
    }
    private interface OnTargetPicked {
        void onPicked(String targetLang);
    }
}