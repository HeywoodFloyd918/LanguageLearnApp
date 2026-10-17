package com.example.advancedrussian_hebrewdict.ui.training;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;

import com.example.advancedrussian_hebrewdict.util.SpeechHelper;

/**
 * Обёртка над SpeechHelper + ToneGenerator.
 * Тон-генератор используется только для коротких звуков ответа (успех/ошибка).
 * Вся озвучка слов и предложений идёт через единый SpeechHelper —
 * это устраняет наложение двух озвучек.
 */
public class SpeechManager {

    private final SpeechHelper speechHelper;
    private final ToneGenerator toneGenerator;
    private boolean voiceEnabled = true;

    public SpeechManager(Context context) {
        this.speechHelper = new SpeechHelper(context);
        this.toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
    }

    public boolean isVoiceEnabled() { return voiceEnabled; }

    public void setVoiceEnabled(boolean enabled) { this.voiceEnabled = enabled; }

    public void toggleVoice() { voiceEnabled = !voiceEnabled; }

    // ==================== СЛОВО (изучаемый язык) ====================

    /** Озвучивает слово и вызывает onDone после завершения. */
    public void speakWordAndWait(String targetWord, Runnable onDone) {
        if (!voiceEnabled || isEmpty(targetWord)) {
            if (onDone != null) onDone.run();
            return;
        }
        speechHelper.speakAndPlayWithDone(targetWord, new SpeechHelper.SpeechDoneCallback() {
            @Override
            public void onDone() {
                if (onDone != null) onDone.run();
            }
            @Override
            public void onError(String error) {
                if (onDone != null) onDone.run();
            }
        });
    }

    /** Озвучивает слово без ожидания (для кнопки replay / автоозвучки). */
    public void speakWordAsync(String targetWord) {
        if (!voiceEnabled || isEmpty(targetWord)) return;
        speechHelper.speakAndPlay(targetWord, null);
    }

    // ==================== ПРЕДЛОЖЕНИЕ (с ___ вместо слова) ====================

    /** Озвучивает предложение (без ___) и вызывает onDone после завершения. */
    public void speakSentenceAndWait(String sentenceWithBlank, Runnable onDone) {
        if (!voiceEnabled || isEmpty(sentenceWithBlank)) {
            if (onDone != null) onDone.run();
            return;
        }
        String toSpeak = cleanSentence(sentenceWithBlank);
        if (toSpeak.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }
        speechHelper.speakAndPlayWithDone(toSpeak, new SpeechHelper.SpeechDoneCallback() {
            @Override
            public void onDone() {
                if (onDone != null) onDone.run();
            }
            @Override
            public void onError(String error) {
                if (onDone != null) onDone.run();
            }
        });
    }

    /** Озвучивает предложение без ожидания. */
    public void speakSentenceAsync(String sentenceWithBlank) {
        if (!voiceEnabled || isEmpty(sentenceWithBlank)) return;
        String toSpeak = cleanSentence(sentenceWithBlank);
        if (toSpeak.isEmpty()) return;
        speechHelper.speakAndPlay(toSpeak, null);
    }

    // ==================== ЗВУКИ ОТВЕТА ====================

    public void playSuccess() {
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 150);
        }
    }

    public void playError() {
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250);
        }
    }

    // ==================== ОСВОБОЖДЕНИЕ ====================

    public void release() {
        if (speechHelper != null) speechHelper.close();
        if (toneGenerator != null) {
            try { toneGenerator.release(); } catch (Exception ignored) { }
        }
    }

    // ==================== ВНУТРЕННЕЕ ====================

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private String cleanSentence(String sentence) {
        return sentence.replace("___", "").replace("  ", " ").trim();
    }
}