package com.example.learn_dict_cards_languages_mila.ui.training;

import com.example.learn_dict_cards_languages_mila.data.Word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Состояние текущей сессии тренировки.
 *
 * Гарантирует, что одно и то же слово не появится в двух раундах подряд.
 * Хранит историю последних показанных слов (до 5), но ограничивает её
 * так, чтобы всегда оставалось хотя бы 2 кандидата для выбора —
 * иначе на маленьких пулах (4-5 слов) тренировка застряла бы.
 */
public class TrainingSession {

    private static final double WEIGHT_LEARNED = 1.0;
    private static final double WEIGHT_UNLEARNED = 1.2;
    private static final int MAX_CANDIDATES = 30;
    private static final int MAX_RECENT_HISTORY = 5;

    private final List<Word> words = new ArrayList<>();
    private final LinkedList<Integer> recentWordIds = new LinkedList<>();
    private final Random random = new Random();

    public void setWords(List<Word> newWords) {
        words.clear();
        words.addAll(newWords);
        recentWordIds.clear();
    }

    public void resetHistory() {
        recentWordIds.clear();
    }

    public List<Word> getWords() { return words; }

    public boolean isEmpty() { return words.isEmpty(); }

    public int size() { return words.size(); }

    public int countLearned() {
        int c = 0;
        for (Word w : words) if (w.isLearned) c++;
        return c;
    }

    /**
     * Взвешенный случайный выбор слова.
     *
     * Невыученные имеют вес 1.2, выученные — 1.0.
     * Из кандидатов исключаются:
     *  1) слово с id = excludeId (например, слово после ошибки Groq);
     *  2) последние N показанных слов, где N = min(5, words.size() - 2).
     *     Это гарантирует, что всегда остаётся хотя бы 2 кандидата,
     *     и одно и то же слово не появится в следующем раунде.
     */
    public Word pickWeightedRandom(int excludeId) {
        int historyCount = Math.min(recentWordIds.size(),
                Math.max(0, words.size() - 2));

        // Берём последние historyCount id-шников из истории
        Set<Integer> toExclude = new HashSet<>();
        if (historyCount > 0) {
            int start = recentWordIds.size() - historyCount;
            for (int i = start; i < recentWordIds.size(); i++) {
                toExclude.add(recentWordIds.get(i));
            }
        }

        List<Word> candidates = new ArrayList<>();
        for (Word w : words) {
            if (w.id == excludeId) continue;
            if (toExclude.contains(w.id)) continue;
            candidates.add(w);
        }

        // Крайняя страховка: если почему-то пусто, ослабляем фильтр
        if (candidates.isEmpty()) {
            for (Word w : words) {
                if (w.id != excludeId) candidates.add(w);
            }
        }
        if (candidates.isEmpty()) {
            candidates.addAll(words);
        }
        if (candidates.isEmpty()) return null;

        // Взвешенный выбор
        double total = 0;
        for (Word w : candidates) {
            total += w.isLearned ? WEIGHT_LEARNED : WEIGHT_UNLEARNED;
        }
        double r = random.nextDouble() * total;
        double sum = 0;
        Word chosen = candidates.get(candidates.size() - 1);
        for (Word w : candidates) {
            sum += w.isLearned ? WEIGHT_LEARNED : WEIGHT_UNLEARNED;
            if (r <= sum) { chosen = w; break; }
        }

        addToRecent(chosen.id);
        return chosen;
    }

    private void addToRecent(int id) {
        // Если слово уже в истории — убираем дубликат
        recentWordIds.remove((Integer) id);
        recentWordIds.addLast(id);
        while (recentWordIds.size() > MAX_RECENT_HISTORY) {
            recentWordIds.removeFirst();
        }
    }

    public List<String> collectCandidates(Word current) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (current.hebrew != null) seen.add(current.hebrew.trim());

        List<Word> shuffled = new ArrayList<>(words);
        Collections.shuffle(shuffled, random);

        for (Word w : shuffled) {
            if (w.id == current.id) continue;
            if (w.hebrew == null || w.hebrew.trim().isEmpty()) continue;
            String h = w.hebrew.trim();
            if (seen.contains(h)) continue;
            seen.add(h);
            result.add(h);
            if (result.size() >= MAX_CANDIDATES) break;
        }
        return result;
    }

    public List<String> pickRandomDistractors(Word current,
                                              List<String> alreadyChosen,
                                              String missingWord,
                                              int count) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (current.hebrew != null) seen.add(current.hebrew.trim());
        if (alreadyChosen != null) seen.addAll(alreadyChosen);
        if (missingWord != null) seen.add(missingWord);

        List<Word> shuffled = new ArrayList<>(words);
        Collections.shuffle(shuffled, random);

        for (Word w : shuffled) {
            if (result.size() >= count) break;
            if (w.hebrew == null) continue;
            String h = w.hebrew.trim();
            if (h.isEmpty() || seen.contains(h)) continue;
            seen.add(h);
            result.add(h);
        }
        return result;
    }

    public Random getRandom() { return random; }
}