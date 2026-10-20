package com.example.learn_dict_cards_languages_mila.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.learn_dict_cards_languages_mila.R;
import com.example.learn_dict_cards_languages_mila.util.LanguageManager;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        String baseLang = LanguageManager.getBaseLang(newBase);
        Locale locale = new Locale(baseLang);
        Locale.setDefault(locale);

        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.setLocale(locale);
        Context localizedContext = newBase.createConfigurationContext(config);
        super.attachBaseContext(localizedContext);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Устанавливаем динамическое название приложения
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(LanguageManager.getAppName(this));
        }
    }
}