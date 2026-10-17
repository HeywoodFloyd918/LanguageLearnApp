package com.example.advancedrussian_hebrewdict.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.advancedrussian_hebrewdict.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Озвучка изучаемого слова через Azure Speech.
 * Голос берётся из текущего ИЗУЧАЕМОГО языка.
 */
public class SpeechHelper {
    private static final String TAG = "SpeechHelper";
    private static final String API_KEY = BuildConfig.SPEECH_API_KEY;
    private static final String REGION = BuildConfig.SPEECH_REGION;

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final MediaType XML = MediaType.parse("application/ssml+xml");

    private Context context;
    private MediaPlayer currentPlayer;
    private int generation = 0;

    public SpeechHelper() { }

    public SpeechHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public interface SpeechCallback {
        void onSuccess(byte[] audioData);
        void onError(String error);
    }

    public interface SpeechDoneCallback {
        void onDone();
        void onError(String error);
    }

    // ---------- Публичные методы ----------

    /** Запрос + воспроизведение с текущим изучаемым языком. */
    public void speakAndPlay(String text, SpeechCallback callback) {
        LanguageManager.Language target = LanguageManager.getTarget(context);
        speakAndPlay(text, target, callback);
    }

    /** Совместимость со старым именем. */
    public void speakHebrew(String text, SpeechCallback callback) {
        speakAndPlay(text, callback);
    }

    public void speakAndPlay(String text, LanguageManager.Language lang, SpeechCallback callback) {
        speak(text, lang, new SpeechCallback() {
            @Override
            public void onSuccess(byte[] audioData) {
                playAudio(audioData, null);
                if (callback != null) callback.onSuccess(audioData);
            }
            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void speakAndPlayWithDone(String text, SpeechDoneCallback callback) {
        LanguageManager.Language target = LanguageManager.getTarget(context);
        speakAndPlayWithDone(text, target, callback);
    }

    public void speakAndPlayWithDone(String text, LanguageManager.Language lang,
                                     SpeechDoneCallback callback) {
        speak(text, lang, new SpeechCallback() {
            @Override
            public void onSuccess(byte[] audioData) {
                playAudio(audioData, callback);
            }
            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void speak(String text, LanguageManager.Language lang, SpeechCallback callback) {
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.startsWith("ВАШ")) {
            if (callback != null) callback.onError("Speech key not configured");
            return;
        }

        final int myGen = ++generation;
        stopCurrentPlayer();

        String url = "https://" + REGION + ".tts.speech.microsoft.com/cognitiveservices/v1";
        String ssml = "<speak version='1.0' xml:lang='" + lang.speechLocale + "'>" +
                "<voice name='" + lang.speechVoice + "'>" +
                text +
                "</voice></speak>";

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(ssml, XML))
                .addHeader("Ocp-Apim-Subscription-Key", API_KEY)
                .addHeader("Ocp-Apim-Subscription-Region", REGION)
                .addHeader("Content-Type", "application/ssml+xml")
                .addHeader("X-Microsoft-OutputFormat", "audio-16khz-128kbitrate-mono-mp3")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    if (myGen != generation) return;
                    if (callback != null) callback.onError("Network: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    mainHandler.post(() -> {
                        if (myGen != generation) return;
                        if (callback != null) callback.onError("HTTP " + response.code() + ": " + errBody);
                    });
                    return;
                }
                byte[] audioBytes = response.body() != null ? response.body().bytes() : new byte[0];
                mainHandler.post(() -> {
                    if (myGen != generation) return;
                    if (callback != null) callback.onSuccess(audioBytes);
                });
            }
        });
    }

    // ---------- Внутренняя работа с MediaPlayer ----------

    private void stopCurrentPlayer() {
        if (currentPlayer != null) {
            try { if (currentPlayer.isPlaying()) currentPlayer.stop(); } catch (Exception ignored) { }
            try { currentPlayer.reset(); } catch (Exception ignored) { }
            try { currentPlayer.release(); } catch (Exception ignored) { }
            currentPlayer = null;
        }
    }

    private void playAudio(byte[] audioData, SpeechDoneCallback doneCallback) {
        if (context == null || audioData == null || audioData.length == 0) {
            if (doneCallback != null) doneCallback.onDone();
            return;
        }
        try {
            stopCurrentPlayer();

            File tempFile = File.createTempFile("tts_audio", ".mp3", context.getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(audioData);
            fos.close();

            currentPlayer = new MediaPlayer();
            final MediaPlayer thisPlayer = currentPlayer;
            thisPlayer.setDataSource(tempFile.getAbsolutePath());
            thisPlayer.setOnPreparedListener(MediaPlayer::start);
            thisPlayer.setOnCompletionListener(mp -> {
                try { mp.release(); } catch (Exception ignored) { }
                if (currentPlayer == thisPlayer) currentPlayer = null;
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
                if (doneCallback != null) doneCallback.onDone();
            });
            thisPlayer.setOnErrorListener((mp, what, extra) -> {
                try { mp.release(); } catch (Exception ignored) { }
                if (currentPlayer == thisPlayer) currentPlayer = null;
                if (doneCallback != null) doneCallback.onError("MediaPlayer: " + what);
                return true;
            });
            thisPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Play error: " + e.getMessage());
            if (doneCallback != null) doneCallback.onError(e.getMessage());
        }
    }

    public void close() {
        generation++;
        stopCurrentPlayer();
    }
}