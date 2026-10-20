package com.example.learn_dict_cards_languages_mila.util;

import android.os.Handler;
import android.os.Looper;

import com.example.learn_dict_cards_languages_mila.BuildConfig;
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

public class GroqHelper {

    private static final String API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "openai/gpt-oss-120b";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public interface SentenceCallback {
        void onSuccess(String sentenceWithBlank, String missingWord,
                       String nativeTranslation, List<String> chosenDistractors);
        void onError(String error);
    }

    public static class VerbForms {
        public String infinitive = "";
        public String presentM = "";
        public String presentF = "";
        public String presentPl = "";
        public String futureM = "";
    }

    public interface VerbFormsCallback {
        void onSuccess(VerbForms forms);
        void onError(String error);
    }

    private String getLangName(String code) {
        switch(code) {
            case "ru": return "Russian";
            case "en": return "English";
            case "es": return "Spanish";
            case "he": return "Hebrew";
            default: return code;
        }
    }

    public void generateSentence(String targetWord, String nativeWord,
                                 String targetLang, String baseLang,
                                 List<String> candidates, SentenceCallback callback) {
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.startsWith("ВАШ")) {
            callback.onError("Groq API-ключ не настроен");
            return;
        }

        String targetLangName = getLangName(targetLang);
        String baseLangName = getLangName(baseLang);

        String prompt = "You are a helpful language learning assistant.\n" +
                "Task: Create a short, natural sentence in " + targetLangName + " using the word '" + targetWord + "' (which means '" + nativeWord + "' in " + baseLangName + ").\n" +
                "1. Replace the target word in the sentence with '___'.\n" +
                "2. Translate the generated sentence into " + baseLangName + ". Make sure to replace the translated target word with '___' in the translation as well.\n" +
                "3. Provide 3 distractor words in " + targetLangName + " that are grammatically plausible but incorrect in this context.\n" +
                "Strictly return a JSON object with this exact format:\n" +
                "{\n" +
                "  \"sentence\": \"The sentence with ___ in " + targetLangName + "\",\n" +
                "  \"missing_word\": \"The exact word that goes in the blank\",\n" +
                "  \"translation\": \"The translation with ___ in " + baseLangName + "\",\n" +
                "  \"distractors\": [\"distractor1\", \"distractor2\", \"distractor3\"]\n" +
                "}";

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "You are a strict language assistant. Answer only with valid JSON.");

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(sysMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.add("messages", messages);
        body.addProperty("temperature", 0.4);

        Request request = new Request.Builder()
                .url(URL)
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("API: " + response.code()));
                    return;
                }
                try {
                    String respBody = response.body().string();
                    JsonObject json = JsonParser.parseString(respBody).getAsJsonObject();
                    String content = json.getAsJsonArray("choices").get(0).getAsJsonObject()
                            .getAsJsonObject("message").get("content").getAsString();

                    String jsonString = extractJson(content);
                    JsonObject inner = JsonParser.parseString(jsonString).getAsJsonObject();

                    String sentence = inner.get("sentence").getAsString();
                    String missingWord = inner.get("missing_word").getAsString();
                    String translation = inner.get("translation").getAsString();

                    List<String> distractors = new ArrayList<>();
                    if (inner.has("distractors")) {
                        JsonArray arr = inner.getAsJsonArray("distractors");
                        for (int i = 0; i < arr.size(); i++) {
                            distractors.add(arr.get(i).getAsString());
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(sentence, missingWord, translation, distractors));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Parse: " + e.getMessage()));
                }
            }
        });
    }

    public void getVerbForms(String hebrewVerb, String originalWord, String targetLang, VerbFormsCallback callback) {
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.startsWith("ВАШ")) {
            callback.onError("Groq API-ключ не настроен");
            return;
        }

        String prompt;
        if (hebrewVerb != null && !hebrewVerb.isEmpty()) {
            prompt = "Ты — строгий лингвистический ассистент.\n" +
                    "Дан глагол на иврите: '" + hebrewVerb + "'.\n" +
                    "1. Найди его инфинитив.\n" +
                    "2. Найди форму настоящего времени для 'он' (м.р., ед.ч.).\n" +
                    "3. Найди форму настоящего времени для 'она' (ж.р., ед.ч.).\n" +
                    "4. Найди форму настоящего времени для 'они' (м.р., мн.ч.).\n" +
                    "5. Найди форму будущего времени для 'ты' (м.р., ед.ч.).\n" +
                    "Формат ответа — СТРОГО JSON-объект:\n" +
                    "{\n" +
                    "  \"infinitive\": \"слово\",\n" +
                    "  \"present_m\": \"слово\",\n" +
                    "  \"present_f\": \"слово\",\n" +
                    "  \"present_pl\": \"слово\",\n" +
                    "  \"future_m\": \"слово\"\n" +
                    "}";
        } else {
            prompt = "Ты — строгий лингвистический ассистент.\n" +
                    "Переведи глагол '" + originalWord + "' на иврит и найди его формы.\n" +
                    "1. Найди его инфинитив.\n" +
                    "2. Найди форму настоящего времени для 'он' (м.р., ед.ч.).\n" +
                    "3. Найди форму настоящего времени для 'она' (ж.р., ед.ч.).\n" +
                    "4. Найди форму настоящего времени для 'они' (м.р., мн.ч.).\n" +
                    "5. Найди форму будущего времени для 'ты' (м.р., ед.ч.).\n" +
                    "Формат ответа — СТРОГО JSON-объект:\n" +
                    "{\n" +
                    "  \"infinitive\": \"слово\",\n" +
                    "  \"present_m\": \"слово\",\n" +
                    "  \"present_f\": \"слово\",\n" +
                    "  \"present_pl\": \"слово\",\n" +
                    "  \"future_m\": \"слово\"\n" +
                    "}";
        }

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "Ты — строгий лингвистический ассистент. Отвечай только валидным JSON.");

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(sysMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.add("messages", messages);
        body.addProperty("temperature", 0.1);

        Request request = new Request.Builder()
                .url(URL)
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("API: " + response.code()));
                    return;
                }
                try {
                    String respBody = response.body().string();
                    JsonObject json = JsonParser.parseString(respBody).getAsJsonObject();
                    String content = json.getAsJsonArray("choices").get(0).getAsJsonObject()
                            .getAsJsonObject("message").get("content").getAsString();

                    String jsonString = extractJson(content);
                    JsonObject inner = JsonParser.parseString(jsonString).getAsJsonObject();

                    VerbForms forms = new VerbForms();
                    if (inner.has("infinitive")) forms.infinitive = inner.get("infinitive").getAsString().trim();
                    if (inner.has("present_m")) forms.presentM = inner.get("present_m").getAsString().trim();
                    if (inner.has("present_f")) forms.presentF = inner.get("present_f").getAsString().trim();
                    if (inner.has("present_pl")) forms.presentPl = inner.get("present_pl").getAsString().trim();
                    if (inner.has("future_m")) forms.futureM = inner.get("future_m").getAsString().trim();

                    mainHandler.post(() -> callback.onSuccess(forms));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Parse: " + e.getMessage()));
                }
            }
        });
    }

    private String extractJson(String text) {
        if (text == null) return "";
        text = text.trim();
        if (text.startsWith("```")) {
            int nl = text.indexOf('\n');
            int close = text.lastIndexOf("```");
            if (nl > 0 && close > nl) text = text.substring(nl + 1, close).trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : "";
    }

    public void close() { }
}