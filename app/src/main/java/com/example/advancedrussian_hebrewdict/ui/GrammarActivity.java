package com.example.advancedrussian_hebrewdict.ui;

import android.os.Bundle;

import com.example.advancedrussian_hebrewdict.R;
import com.example.advancedrussian_hebrewdict.util.LanguageManager;

public class GrammarActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String target = LanguageManager.getTargetCode(this);
        String base = LanguageManager.getBaseLang(this);

        // Ищем layout по комбинации target + base
        // Для новых языков, где нет специфичного layout — используем RU-версию
        int layoutId;
        String title;
        if ("en".equals(target)) {
            layoutId = R.layout.activity_grammar_en;
            title = getString(R.string.grammar_title_en);
        } else if ("es".equals(target)) {
            layoutId = "en".equals(base)
                    ? R.layout.activity_grammar_es_en
                    : R.layout.activity_grammar_es;
            title = getString(R.string.grammar_title_es);
        } else {
            layoutId = "en".equals(base)
                    ? R.layout.activity_grammar_he_en
                    : R.layout.activity_grammar;
            title = getString(R.string.grammar_title_he);
        }

        setContentView(layoutId);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
    }
}