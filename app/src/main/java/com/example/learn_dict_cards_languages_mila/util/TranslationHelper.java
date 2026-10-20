package com.example.learn_dict_cards_languages_mila.util;

import android.os.Handler;
import android.os.Looper;

import com.example.learn_dict_cards_languages_mila.BuildConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final GroqHelper groqHelper = new GroqHelper();

    private static final int MAX_RESULTS = 5;

    public static class TranslationResult {
        public final String text;
        public final float probability;

        public TranslationResult(String text, float probability) {
            this.text = text;
            this.probability = probability;
        }
    }

    public interface TranslationCallback {
        void onSuccess(List<TranslationResult> results);
        void onError(String error);
    }

    public interface SimpleTranslationCallback {
        void onSuccess(String translatedText);
        void onError(String error);
    }

    public void translate(String text, String fromLang, String toLang,
                          TranslationCallback callback) {
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.startsWith("ВАШ")) {
            callback.onError("API-ключ не настроен.");
            return;
        }

        final String cleanedText = normalizeInput(text, fromLang);
        final boolean isPhrase = cleanedText.contains(" ");
        final boolean sourceIsVerb = isVerbHeuristic(text, cleanedText, fromLang);

        executeTranslate(cleanedText, fromLang, toLang, new SimpleTranslationCallback() {
            @Override
            public void onSuccess(String mainTranslation) {
                final Map<String, Float> mergedMap = new HashMap<>();
                final String cleanMain = applyLowerCase(mainTranslation, toLang);

                if (isPhrase) {
                    mergedMap.put(cleanMain, 0.99f);
                    callback.onSuccess(convertToSortedList(mergedMap, toLang, false));
                    return;
                }

                mergedMap.put(cleanMain, 0.75f);

                dictionaryHelper.lookup(cleanedText, fromLang, toLang, new AzureDictionaryHelper.DictionaryCallback() {
                    @Override
                    public void onSuccess(List<AzureDictionaryHelper.DictionaryEntry> directEntries) {
                        String bestVerb = null;

                        if (directEntries != null) {
                            for (AzureDictionaryHelper.DictionaryEntry entry : directEntries) {
                                String targetWord = applyLowerCase(entry.translation, toLang);
                                float confidence = 0.85f;
                                boolean isVerbEntry = false;
                                boolean isNounEntry = false;

                                if (entry.posTag != null) {
                                    String tag = entry.posTag.toUpperCase();
                                    isVerbEntry = tag.contains("VERB") || tag.contains("INFINITIVE");
                                    isNounEntry = tag.contains("NOUN") || tag.contains("ADJ") || tag.contains("ADV") || tag.contains("PRON");
                                }

                                // Строгая фильтрация по части речи
                                if (sourceIsVerb) {
                                    if (isVerbEntry) {
                                        confidence = 0.99f;
                                        if (bestVerb == null) bestVerb = targetWord;
                                    } else if (isNounEntry) {
                                        confidence = 0.40f; // Штраф для существительных
                                    }
                                } else {
                                    if (isNounEntry) confidence = 0.99f;
                                    if (isVerbEntry) confidence = 0.60f; // Штраф для глаголов
                                }

                                if (targetWord.equals(cleanMain)) {
                                    if (sourceIsVerb && isVerbEntry) confidence = 0.99f;
                                    else if (!sourceIsVerb && isNounEntry) confidence = 0.99f;
                                    else confidence = Math.max(confidence, 0.75f);
                                }

                                if (!mergedMap.containsKey(targetWord) || mergedMap.get(targetWord) < confidence) {
                                    mergedMap.put(targetWord, confidence);
                                }
                            }
                        }

                        // Для иврита вызываем ИИ, чтобы получить спряжения (передаем найденный глагол bestVerb)
                        if (sourceIsVerb && "he".equals(toLang)) {
                            callGroqForAllForms(bestVerb, cleanedText, toLang, mergedMap, callback);
                        } else {
                            callback.onSuccess(convertToSortedList(mergedMap, toLang, false));
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (sourceIsVerb && "he".equals(toLang)) {
                            callGroqForAllForms(null, cleanedText, toLang, mergedMap, callback);
                        } else {
                            callback.onSuccess(convertToSortedList(mergedMap, toLang, false));
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private boolean isVerbHeuristic(String originalText, String cleanedText, String lang) {
        String lower = cleanedText.toLowerCase();
        String originalLower = originalText.toLowerCase().trim();

        if ("ru".equals(lang)) {
            return lower.endsWith("ть") || lower.endsWith("ться") || lower.endsWith("ти") || lower.endsWith("чь");
        }
        if ("en".equals(lang)) {
            return originalLower.startsWith("to ");
        }
        if ("es".equals(lang)) {
            return lower.endsWith("ar") || lower.endsWith("er") || lower.endsWith("ir");
        }
        return false;
    }

    private void callGroqForAllForms(String bestVerb, String originalText, String toLang, Map<String, Float> mergedMap, TranslationCallback callback) {
        groqHelper.getVerbForms(bestVerb, originalText, toLang, new GroqHelper.VerbFormsCallback() {
            @Override
            public void onSuccess(GroqHelper.VerbForms forms) {
                if (!forms.infinitive.isEmpty()) {
                    mergedMap.put(applyLowerCase(forms.infinitive, toLang), 0.99f);
                }
                if (!forms.presentM.isEmpty() && !mergedMap.containsKey(forms.presentM))
                    mergedMap.put(applyLowerCase(forms.presentM, toLang), 0.90f);
                if (!forms.presentF.isEmpty() && !mergedMap.containsKey(forms.presentF))
                    mergedMap.put(applyLowerCase(forms.presentF, toLang), 0.85f);
                if (!forms.presentPl.isEmpty() && !mergedMap.containsKey(forms.presentPl))
                    mergedMap.put(applyLowerCase(forms.presentPl, toLang), 0.85f);
                if (!forms.futureM.isEmpty() && !mergedMap.containsKey(forms.futureM))
                    mergedMap.put(applyLowerCase(forms.futureM, toLang), 0.80f);

                callback.onSuccess(convertToSortedList(mergedMap, toLang, true));
            }

            @Override
            public void onError(String error) {
                callback.onSuccess(convertToSortedList(mergedMap, toLang, true));
            }
        });
    }

    private String normalizeInput(String text, String lang) {
        if (text == null) return "";
        String cleaned = text.trim();
        String lower = cleaned.toLowerCase();

        if ("en".equals(lang)) {
            if (lower.startsWith("to ")) cleaned = cleaned.substring(3);
            else if (lower.startsWith("the ")) cleaned = cleaned.substring(4);
            else if (lower.startsWith("a ")) cleaned = cleaned.substring(2);
            else if (lower.startsWith("an ")) cleaned = cleaned.substring(3);
        } else if ("es".equals(lang)) {
            if (lower.startsWith("el ")) cleaned = cleaned.substring(3);
            else if (lower.startsWith("la ")) cleaned = cleaned.substring(3);
            else if (lower.startsWith("los ")) cleaned = cleaned.substring(4);
            else if (lower.startsWith("las ")) cleaned = cleaned.substring(4);
            else if (lower.startsWith("un ")) cleaned = cleaned.substring(3);
            else if (lower.startsWith("una ")) cleaned = cleaned.substring(4);
        }
        return cleaned.trim();
    }

    private String removeArticle(String word, String toLang) {
        if ("he".equals(toLang) && word.length() > 1 && word.startsWith("ה")) return word.substring(1);
        if ("en".equals(toLang) && word.startsWith("the ")) return word.substring(4);
        if ("es".equals(toLang)) {
            if (word.startsWith("el ")) return word.substring(3);
            if (word.startsWith("la ")) return word.substring(3);
            if (word.startsWith("los ")) return word.substring(4);
            if (word.startsWith("las ")) return word.substring(4);
        }
        return word;
    }

    private List<TranslationResult> convertToSortedList(Map<String, Float> map, String toLang, boolean isVerb) {
        List<TranslationResult> results = new ArrayList<>();

        Set<String> existingBases = new HashSet<>();
        for (String word : map.keySet()) {
            if (!isVerb) {
                existingBases.add(removeArticle(word, toLang));
            }
        }

        for (Map.Entry<String, Float> entry : map.entrySet()) {
            String word = entry.getKey();

            if (!isVerb) {
                String baseWord = removeArticle(word, toLang);
                if (!word.equals(baseWord) && existingBases.contains(baseWord)) {
                    if (map.getOrDefault(baseWord, 0f) >= 0.85f) {
                        continue;
                    }
                }
            }

            if (word.length() < 35) {
                results.add(new TranslationResult(word, entry.getValue()));
            }
        }

        Collections.sort(results, (o1, o2) -> Float.compare(o2.probability, o1.probability));

        // ЖЕСТКИЙ ФИЛЬТР: если топ-результат идеален (0.99), удаляем весь мусор (ниже 0.85)
        if (!results.isEmpty() && results.get(0).probability >= 0.95f) {
            List<TranslationResult> strictResults = new ArrayList<>();
            for (TranslationResult res : results) {
                if (res.probability >= 0.85f) {
                    strictResults.add(res);
                }
            }
            results = strictResults;
        }

        if (results.size() > MAX_RESULTS) {
            return results.subList(0, MAX_RESULTS);
        }
        return results;
    }

    public void translateSimple(String text, String fromLang, String toLang,
                                SimpleTranslationCallback callback) {
        executeTranslate(text, fromLang, toLang, callback);
    }

    private void executeTranslate(String text, String fromLang, String toLang,
                                  SimpleTranslationCallback callback) {
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
                    String translated = translations.get(0).getAsJsonObject()
                            .get("text").getAsString();
                    String result = applyLowerCase(translated, toLang);
                    mainHandler.post(() -> callback.onSuccess(result));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Парсинг: " + e.getMessage()));
                }
            }
        });
    }

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
        if (groqHelper != null) groqHelper.close();
    }
}