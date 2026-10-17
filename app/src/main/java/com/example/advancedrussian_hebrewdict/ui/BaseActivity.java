package com.example.advancedrussian_hebrewdict.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.advancedrussian_hebrewdict.R;
import com.example.advancedrussian_hebrewdict.util.LanguageManager;

import java.util.Locale;

/**
 * Базовая активность. Автоматически подменяет локаль на выбранный
 * пользователем родной язык (RU/EN) и устанавливает заголовок
 * ActionBar из strings.xml (app_name). Дочерние активности могут
 * переопределить заголовок в своём onCreate после super.onCreate.
 */
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

        // По умолчанию заголовок = app_name на текущем родном языке.
        // Если дочерняя активность хочет свой заголовок — она установит его
        // после вызова super.onCreate() в своём onCreate.
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.app_name));
        }
    }
}