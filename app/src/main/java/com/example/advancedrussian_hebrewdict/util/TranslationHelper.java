package com.example.advancedrussian_hebrewdict.util;

import android.os.Handler;
import android.os.Looper;

import com.example.advancedrussian_hebrewdict.BuildConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TranslationHelper {

    private static final String API_KEY = BuildConfig.AZURE_TRANSLATOR_KEY;
    private static final String REGION = BuildConfig.AZURE_TRANSLATOR_REGION;
    private static final String BASE_URL =
            "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final AzureDictionaryHelper dictionaryHelper = new AzureDictionaryHelper();

    public interface TranslationCallback {
        void onSuccess(List<String> translatedTexts);
        void onError(String error);
    }

    public interface SimpleTranslationCallback {
        void onSuccess(String translatedText);
        void onError(String error);
    }

    public void translate(String text, String fromLang, String toLang,
                          TranslationCallback callback) {
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.startsWith("ВАШ")
                || REGION == null || REGION.isEmpty() || REGION.startsWith("ВАШ")) {
            callback.onError("API-ключ или регион не настроены.");
            return;
        }

        String url = BASE_URL + "&from=" + fromLang + "&to=" + toLang;

        JsonObject item = new JsonObject();
        item.addProperty("Text", text.trim());
        JsonArray bodyArray = new JsonArray();
        bodyArray.add(item);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(bodyArray.toString(), JSON))
                .addHeader("Ocp-Apim-Subscription-Key", API_KEY)
                .addHeader("Ocp-Apim-Subscription-Region", REGION)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Сеть: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("Azure API: " + response.code()));
                    return;
                }
                try {
                    JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
                    JsonObject firstItem = jsonArray.get(0).getAsJsonObject();
                    JsonArray translations = firstItem.getAsJsonArray("translations");
                    String rawTranslation = translations.get(0).getAsJsonObject()
                            .get("text").getAsString();

                    // Создаём final-переменную после понижения регистра —
                    // иначе Java не разрешит использовать её в анонимном классе ниже
                    final String azureTranslation = applyLowerCase(rawTranslation, toLang);

                    // Dictionary Lookup только для иврита
                    if (!"he".equals(toLang)) {
                        List<String> single = new ArrayList<>();
                        single.add(azureTranslation);
                        mainHandler.post(() -> callback.onSuccess(single));
                        return;
                    }

                    dictionaryHelper.lookup(text, fromLang, toLang,
                            new AzureDictionaryHelper.DictionaryCallback() {
                                @Override
                                public void onSuccess(List<AzureDictionaryHelper.DictionaryEntry> entries) {
                                    Set<String> merged = new LinkedHashSet<>();
                                    merged.add(azureTranslation);
                                    for (AzureDictionaryHelper.DictionaryEntry entry : entries) {
                                        if (!entry.translation.equals(azureTranslation)) {
                                            merged.add(entry.translation);
                                        }
                                    }
                                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>(merged)));
                                }
                                @Override
                                public void onError(String error) {
                                    List<String> single = new ArrayList<>();
                                    single.add(azureTranslation);
                                    mainHandler.post(() -> callback.onSuccess(single));
                                }
                            });
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Парсинг Azure: " + e.getMessage()));
                }
            }
        });
    }

    public void translateSimple(String text, String fromLang, String toLang,
                                SimpleTranslationCallback callback) {
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.startsWith("ВАШ")) {
            callback.onError("API-ключ не настроен.");
            return;
        }

        String url = BASE_URL + "&from=" + fromLang + "&to=" + toLang;

        JsonObject item = new JsonObject();
        item.addProperty("Text", text.trim());
        JsonArray bodyArray = new JsonArray();
        bodyArray.add(item);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(bodyArray.toString(), JSON))
                .addHeader("Ocp-Apim-Subscription-Key", API_KEY)
                .addHeader("Ocp-Apim-Subscription-Region", REGION)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Сеть: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("Azure API: " + response.code()));
                    return;
                }
                try {
                    JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
                    JsonObject firstItem = jsonArray.get(0).getAsJsonObject();
                    JsonArray translations = firstItem.getAsJsonArray("translations");
                    String raw = translations.get(0).getAsJsonObject()
                            .get("text").getAsString();
                    final String result = applyLowerCase(raw, toLang);
                    mainHandler.post(() -> callback.onSuccess(result));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Парсинг: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Приводит первую букву к строчной для английского и испанского.
     * Для иврита возвращает текст без изменений (иврит регистронезависим).
     */
    private String applyLowerCase(String text, String lang) {
        if (text == null || text.isEmpty()) return text;
        if ("he".equals(lang)) return text;
        char first = text.charAt(0);
        if (Character.isUpperCase(first)) {
            return Character.toLowerCase(first) + text.substring(1);
        }
        return text;
    }

    public void close() {
        if (dictionaryHelper != null) dictionaryHelper.close();
    }
}