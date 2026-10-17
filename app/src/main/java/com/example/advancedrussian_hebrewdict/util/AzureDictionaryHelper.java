package com.example.advancedrussian_hebrewdict.util;

import android.os.Handler;
import android.os.Looper;

import com.example.advancedrussian_hebrewdict.BuildConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AzureDictionaryHelper {

    private static final String API_KEY = BuildConfig.AZURE_TRANSLATOR_KEY;
    private static final String REGION = BuildConfig.AZURE_TRANSLATOR_REGION;
    private static final String BASE_URL =
            "https://api.cognitive.microsofttranslator.com/dictionary/lookup?api-version=3.0";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public interface DictionaryCallback {
        void onSuccess(List<DictionaryEntry> entries);
        void onError(String error);
    }

    public static class DictionaryEntry {
        public String translation;
        public String posTag;
        public double confidence;
    }

    public void lookup(String word, String fromLang, String toLang, DictionaryCallback callback) {
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.startsWith("ВАШ")) {
            callback.onError("API-ключ не настроен");
            return;
        }

        String url = BASE_URL + "&from=" + fromLang + "&to=" + toLang;

        JsonObject item = new JsonObject();
        item.addProperty("Text", word.trim());
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
                    mainHandler.post(() -> callback.onError("API: " + response.code()));
                    return;
                }
                try {
                    JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
                    JsonObject firstItem = jsonArray.get(0).getAsJsonObject();
                    JsonArray translations = firstItem.getAsJsonArray("translations");

                    List<DictionaryEntry> entries = new ArrayList<>();
                    for (int i = 0; i < translations.size(); i++) {
                        JsonObject t = translations.get(i).getAsJsonObject();
                        DictionaryEntry entry = new DictionaryEntry();
                        entry.translation = t.get("displayTarget").getAsString();
                        if (t.has("posTag")) entry.posTag = t.get("posTag").getAsString();
                        if (t.has("confidence")) entry.confidence = t.get("confidence").getAsDouble();
                        entries.add(entry);
                    }
                    mainHandler.post(() -> callback.onSuccess(entries));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Парсинг: " + e.getMessage()));
                }
            }
        });
    }

    public void close() { }
}