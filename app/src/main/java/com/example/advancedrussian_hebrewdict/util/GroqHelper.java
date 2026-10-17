package com.example.advancedrussian_hebrewdict.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.advancedrussian_hebrewdict.BuildConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroqHelper {

    private static final String TAG = "GroqHelper";
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

    private interface DistractorValidationCallback {
        void onValidated(List<String> validDistractors);
        void onError(String error);
    }

    /**
     * @param targetWord   изучаемое слово
     * @param nativeWord   перевод слова на родной язык пользователя
     * @param targetLang   код изучаемого языка (he/en/es)
     * @param baseLang     код родного языка (ru/en)
     */
    public void generateSentence(String targetWord, String nativeWord,
                                 String targetLang, String baseLang,
                                 List<String> candidates, SentenceCallback callback) {
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.startsWith("ВАШ")) {
            callback.onError("Groq API-ключ не настроен");
            return;
        }

        boolean needDistractors = (candidates != null && candidates.size() >= 3);
        String prompt = buildPrompt(targetLang, baseLang, candidates, needDistractors);

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", prompt);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content",
                "Word: " + targetWord + "\nMeaning (" + baseLang + "): " +
                        (nativeWord != null ? nativeWord : ""));

        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
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
                mainHandler.post(() -> callback.onError("Network: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("Groq API: " + response.code()));
                    return;
                }
                try {
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                    JsonArray choices = json.getAsJsonArray("choices");
                    JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                    String content = message.get("content").getAsString();

                    String jsonString = extractJson(content);
                    JsonObject inner = JsonParser.parseString(jsonString).getAsJsonObject();

                    String sentence = inner.get("sentence").getAsString().trim();
                    String missingWord = inner.get("missingWord").getAsString().trim();
                    String nativeTranslation = inner.has("russianTranslation")
                            ? inner.get("russianTranslation").getAsString().trim() : "";

                    if (!sentence.contains("___")) {
                        if (sentence.contains("__")) {
                            sentence = sentence.replace("__", "___");
                        } else if (missingWord.length() > 1 && sentence.contains(missingWord)) {
                            sentence = sentence.replaceFirst(Pattern.quote(missingWord), "___");
                        }
                    }

                    if (!nativeTranslation.contains("___")) {
                        String fixed = tryFixTranslation(nativeTranslation, nativeWord);
                        if (fixed != null) nativeTranslation = fixed;
                    }

                    List<String> chosenDistractors = parseDistractors(inner, missingWord);

                    final String fSentence = sentence;
                    final String fMissing = missingWord;
                    final String fTranslation = nativeTranslation;
                    final List<String> fDistractors = chosenDistractors;

                    if (needDistractors && fDistractors.size() >= 3) {
                        validateDistractors(fSentence, fMissing, targetLang, baseLang,
                                fDistractors, candidates, new DistractorValidationCallback() {
                                    @Override
                                    public void onValidated(List<String> validDistractors) {
                                        mainHandler.post(() -> callback.onSuccess(
                                                fSentence, fMissing, fTranslation, validDistractors));
                                    }
                                    @Override
                                    public void onError(String error) {
                                        mainHandler.post(() -> callback.onSuccess(
                                                fSentence, fMissing, fTranslation, fDistractors));
                                    }
                                });
                    } else {
                        mainHandler.post(() -> callback.onSuccess(
                                fSentence, fMissing, fTranslation, fDistractors));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parse: " + e.getMessage());
                    mainHandler.post(() -> callback.onError("Parse: " + e.getMessage()));
                }
            }
        });
    }

    private String tryFixTranslation(String translation, String nativeWord) {
        if (translation == null || translation.isEmpty()) return null;
        if (nativeWord != null && !nativeWord.isEmpty()) {
            String[] parts = nativeWord.split("[,;()]");
            for (String p : parts) {
                String word = p.trim().toLowerCase();
                if (word.isEmpty()) continue;
                String lower = translation.toLowerCase();
                int idx = lower.indexOf(word);
                if (idx >= 0) {
                    return translation.substring(0, idx) + "___" +
                            translation.substring(idx + word.length());
                }
            }
        }
        return null;
    }

    private String buildPrompt(String targetLang, String baseLang,
                               List<String> candidates, boolean needDistractors) {
        String targetName = langName(targetLang, "en");
        String nativeName = langName(baseLang, "en");

        StringBuilder p = new StringBuilder();
        p.append("You are a language-learning assistant for beginners (A1-A2). ")
                .append("You are given a word in ").append(targetName)
                .append(" and its translation into ").append(nativeName).append(".\n\n")

                .append("TASK: create ONE meaningful sentence in ").append(targetName).append(".\n\n")

                .append("HARD REQUIREMENTS for the sentence:\n")
                .append("1. LENGTH: 3 to 6 words (including the gap).\n")
                .append("2. Use only simple, common words.\n")
                .append("3. No idioms or rare expressions.\n")
                .append("4. Simple syntax.\n")
                .append("5. The gap word must be a noun, adjective, verb, or adverb. ")
                .append("NOT prepositions/conjunctions/particles/pronouns.\n")
                .append("6. Replace the given word with EXACTLY ___ (three underscores).\n")
                .append("7. The form must agree with the sentence.\n");

        if (needDistractors) {
            p.append("\nDISTRACTOR TASK:\n")
                    .append("The user will see 4 options: 1 correct + 3 wrong.\n")
                    .append("Choose EXACTLY 3 from the candidate list such that each one definitely does ")
                    .append("NOT fit — neither in meaning nor in grammar.\n")
                    .append("Do not choose synonyms or grammatically compatible words.\n")
                    .append("CANDIDATES:\n");
            for (String c : candidates) p.append("• ").append(c).append("\n");
        }

        p.append("\n----------------------------------------\n")
                .append("FIELD russianTranslation (IMPORTANT!):\n")
                .append("This is the translation of the WHOLE sentence into ")
                .append(nativeName).append(".\n")
                .append("At the position of the missing word there MUST be ___ .\n")
                .append("Do NOT translate the missing word separately! Translate the whole sentence.\n\n")
                .append("CORRECT example:\n")
                .append("  sentence: \"אני רואה את ___\"\n")
                .append("  missingWord: \"כלב\"\n")
                .append("  russianTranslation: \"Я вижу ___\"\n\n")
                .append("WRONG example (NOT allowed):\n")
                .append("  russianTranslation: \"собака\"\n")
                .append("----------------------------------------\n\n")

                .append("ANSWER — STRICTLY ONE JSON OBJECT, no Markdown, no comments:\n")
                .append("{\"sentence\": \"...\", \"missingWord\": \"...\", ")
                .append("\"russianTranslation\": \"...\", ")
                .append("\"chosenDistractors\": [\"...\", \"...\", \"...\"]}");
        return p.toString();
    }

    private String langName(String code, String uiLang) {
        boolean ru = "ru".equals(uiLang);
        switch (code) {
            case "he": return ru ? "иврите" : "Hebrew";
            case "en": return ru ? "английском" : "English";
            case "es": return ru ? "испанском" : "Spanish";
            case "ru": return ru ? "русском" : "Russian";
            default:   return code;
        }
    }

    private List<String> parseDistractors(JsonObject inner, String missingWord) {
        List<String> result = new ArrayList<>();
        if (inner.has("chosenDistractors") && inner.get("chosenDistractors").isJsonArray()) {
            JsonArray arr = inner.getAsJsonArray("chosenDistractors");
            for (int i = 0; i < arr.size(); i++) {
                try {
                    String d = arr.get(i).getAsString().trim();
                    if (!d.isEmpty() && !d.equals(missingWord) && !result.contains(d)) {
                        result.add(d);
                    }
                } catch (Exception ignored) { }
            }
        }
        return result;
    }

    private void validateDistractors(String sentence, String missingWord,
                                     String targetLang, String baseLang,
                                     List<String> distractors, List<String> candidates,
                                     DistractorValidationCallback callback) {
        String targetName = langName(targetLang, "en");

        StringBuilder vp = new StringBuilder();
        vp.append("Sentence in ").append(targetName).append(" with a gap: ")
                .append(sentence).append("\n")
                .append("Correct word: ").append(missingWord).append("\n\n")
                .append("Check each word — does it fit in meaning AND grammar?\n");
        for (int i = 0; i < distractors.size(); i++) {
            vp.append(i + 1).append(". ").append(distractors.get(i)).append("\n");
        }
        vp.append("\nReply with JSON: {\"bad\": [\"...\"]}\n")
                .append("In \"bad\" put ONLY words that FIT (they should be removed). ")
                .append("If none fit — {\"bad\": []}");

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "You are a strict checker. Reply with JSON only.");

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", vp.toString());

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
            @Override public void onFailure(Call call, IOException e) {
                callback.onError("Network: " + e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("API: " + response.code()); return;
                }
                try {
                    String respBody = response.body().string();
                    JsonObject json = JsonParser.parseString(respBody).getAsJsonObject();
                    String content = json.getAsJsonArray("choices").get(0).getAsJsonObject()
                            .getAsJsonObject("message").get("content").getAsString();
                    JsonObject inner = JsonParser.parseString(extractJson(content)).getAsJsonObject();

                    List<String> bad = new ArrayList<>();
                    if (inner.has("bad") && inner.get("bad").isJsonArray()) {
                        JsonArray arr = inner.getAsJsonArray("bad");
                        for (int i = 0; i < arr.size(); i++) bad.add(arr.get(i).getAsString().trim());
                    }
                    List<String> valid = new ArrayList<>();
                    for (String d : distractors) if (!bad.contains(d)) valid.add(d);
                    for (String c : candidates) {
                        if (valid.size() >= 3) break;
                        if (c.equals(missingWord) || valid.contains(c) || bad.contains(c)) continue;
                        valid.add(c);
                    }
                    if (valid.size() < 3) callback.onError("Not enough distractors");
                    else callback.onValidated(valid);
                } catch (Exception e) {
                    callback.onError("Parse: " + e.getMessage());
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