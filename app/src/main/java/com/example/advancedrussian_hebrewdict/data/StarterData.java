package com.example.advancedrussian_hebrewdict.data;

import java.util.ArrayList;
import java.util.List;

public class StarterData {

    public static class StarterWord {
        public final String target;
        public final String nativeRu;
        public final String nativeEn;
        public final String commentRu;
        public final String commentEn;

        public StarterWord(String target, String nativeRu, String nativeEn) {
            this(target, nativeRu, nativeEn, null, null);
        }

        public StarterWord(String target, String nativeRu, String nativeEn,
                           String commentRu, String commentEn) {
            this.target = target;
            this.nativeRu = nativeRu;
            this.nativeEn = nativeEn;
            this.commentRu = commentRu;
            this.commentEn = commentEn;
        }

        public String nativeText(String baseLang) {
            return "en".equals(baseLang) ? nativeEn : nativeRu;
        }

        public String commentText(String baseLang) {
            return "en".equals(baseLang) ? commentEn : commentRu;
        }
    }

    public static class StarterFolder {
        public final String nameRu;
        public final String nameEn;
        public final List<StarterWord> words = new ArrayList<>();

        public StarterFolder(String nameRu, String nameEn) {
            this.nameRu = nameRu;
            this.nameEn = nameEn;
        }

        public String name(String baseLang) {
            return "en".equals(baseLang) ? nameEn : nameRu;
        }

        public StarterFolder add(String target, String nativeRu, String nativeEn) {
            words.add(new StarterWord(target, nativeRu, nativeEn));
            return this;
        }
    }

    public static List<StarterFolder> getFor(String targetLang) {
        if (targetLang == null) return new ArrayList<>();
        switch (targetLang) {
            case "en": return english();
            case "es": return spanish();
            case "he": return hebrew();
            default: return new ArrayList<>();
        }
    }

    /** Возвращает true, если имя папки совпадает с шаблонным именем стартовой папки для target-языка. */
    public static boolean isStarterFolderName(String folderName, String targetLang) {
        if (folderName == null) return false;
        List<StarterFolder> folders = getFor(targetLang);
        for (StarterFolder f : folders) {
            if (folderName.equals(f.nameRu) || folderName.equals(f.nameEn)) return true;
        }
        return false;
    }

    // ============================ HEBREW ============================

    private static List<StarterFolder> hebrew() {
        List<StarterFolder> list = new ArrayList<>();

        StarterFolder greetings = new StarterFolder("Приветствия", "Greetings");
        greetings.add("שלום", "мир, здравствуйте", "peace, hello");
        greetings.add("תודה", "спасибо", "thank you");
        greetings.add("בבקשה", "пожалуйста", "please");
        greetings.add("כן", "да", "yes");
        greetings.add("לא", "нет", "no");
        greetings.add("להתראות", "до свидания", "goodbye");
        list.add(greetings);

        StarterFolder basic = new StarterFolder("Базовые слова", "Basic words");
        basic.add("אני", "я", "I");
        basic.add("אתה", "ты (м.р.)", "you (m.)");
        basic.add("את", "ты (ж.р.)", "you (f.)");
        basic.add("הוא", "он", "he");
        basic.add("היא", "она", "she");
        basic.add("אנחנו", "мы", "we");
        basic.add("הם", "они (м.р.)", "they (m.)");
        basic.add("הן", "они (ж.р.)", "they (f.)");
        list.add(basic);

        StarterFolder numbers = new StarterFolder("Числа", "Numbers");
        numbers.add("אחת", "один (ж.р.)", "one (f.)");
        numbers.add("שתיים", "два (ж.р.)", "two (f.)");
        numbers.add("שלוש", "три (ж.р.)", "three (f.)");
        numbers.add("ארבע", "четыре (ж.р.)", "four (f.)");
        numbers.add("חמש", "пять (ж.р.)", "five (f.)");
        numbers.add("שש", "шесть (ж.р.)", "six (f.)");
        numbers.add("שבע", "семь (ж.р.)", "seven (f.)");
        numbers.add("שמונה", "восемь (ж.р.)", "eight (f.)");
        numbers.add("תשע", "девять (ж.р.)", "nine (f.)");
        numbers.add("עשר", "десять (ж.р.)", "ten (f.)");
        list.add(numbers);

        StarterFolder family = new StarterFolder("Семья", "Family");
        family.add("אמא", "мама", "mother");
        family.add("אבא", "папа", "father");
        family.add("אח", "брат", "brother");
        family.add("אחות", "сестра", "sister");
        family.add("בן", "сын", "son");
        family.add("בת", "дочь", "daughter");
        family.add("חבר", "друг", "friend");
        list.add(family);

        StarterFolder food = new StarterFolder("Еда и напитки", "Food and drinks");
        food.add("מים", "вода", "water");
        food.add("לחם", "хлеб", "bread");
        food.add("חלב", "молоко", "milk");
        food.add("תפוח", "яблоко", "apple");
        food.add("תה", "чай", "tea");
        food.add("קפה", "кофе", "coffee");
        list.add(food);

        return list;
    }

    // ============================ ENGLISH ============================

    private static List<StarterFolder> english() {
        List<StarterFolder> list = new ArrayList<>();

        StarterFolder greetings = new StarterFolder("Приветствия", "Greetings");
        greetings.add("hello", "привет", "hello");
        greetings.add("hi", "привет (неформ.)", "hi (informal)");
        greetings.add("goodbye", "до свидания", "goodbye");
        greetings.add("please", "пожалуйста", "please");
        greetings.add("thank you", "спасибо", "thank you");
        greetings.add("sorry", "извините", "sorry");
        greetings.add("yes", "да", "yes");
        greetings.add("no", "нет", "no");
        list.add(greetings);

        StarterFolder basic = new StarterFolder("Базовые слова", "Basic words");
        basic.add("I", "я", "I");
        basic.add("you", "ты, вы", "you");
        basic.add("he", "он", "he");
        basic.add("she", "она", "she");
        basic.add("we", "мы", "we");
        basic.add("they", "они", "they");
        basic.add("this", "это, этот", "this");
        basic.add("that", "то, тот", "that");
        list.add(basic);

        StarterFolder numbers = new StarterFolder("Числа", "Numbers");
        numbers.add("one", "один", "one");
        numbers.add("two", "два", "two");
        numbers.add("three", "три", "three");
        numbers.add("four", "четыре", "four");
        numbers.add("five", "пять", "five");
        numbers.add("six", "шесть", "six");
        numbers.add("seven", "семь", "seven");
        numbers.add("eight", "восемь", "eight");
        numbers.add("nine", "девять", "nine");
        numbers.add("ten", "десять", "ten");
        list.add(numbers);

        StarterFolder family = new StarterFolder("Семья", "Family");
        family.add("mother", "мама", "mother");
        family.add("father", "папа", "father");
        family.add("brother", "брат", "brother");
        family.add("sister", "сестра", "sister");
        family.add("son", "сын", "son");
        family.add("daughter", "дочь", "daughter");
        family.add("friend", "друг", "friend");
        list.add(family);

        StarterFolder food = new StarterFolder("Еда", "Food");
        food.add("water", "вода", "water");
        food.add("bread", "хлеб", "bread");
        food.add("milk", "молоко", "milk");
        food.add("apple", "яблоко", "apple");
        food.add("tea", "чай", "tea");
        food.add("coffee", "кофе", "coffee");
        list.add(food);

        return list;
    }

    // ============================ SPANISH ============================

    private static List<StarterFolder> spanish() {
        List<StarterFolder> list = new ArrayList<>();

        StarterFolder greetings = new StarterFolder("Приветствия", "Greetings");
        greetings.add("hola", "привет", "hello");
        greetings.add("adiós", "до свидания", "goodbye");
        greetings.add("por favor", "пожалуйста", "please");
        greetings.add("gracias", "спасибо", "thank you");
        greetings.add("perdón", "извините", "sorry");
        greetings.add("sí", "да", "yes");
        greetings.add("no", "нет", "no");
        greetings.add("buenos días", "доброе утро", "good morning");
        list.add(greetings);

        StarterFolder basic = new StarterFolder("Базовые слова", "Basic words");
        basic.add("yo", "я", "I");
        basic.add("tú", "ты", "you");
        basic.add("él", "он", "he");
        basic.add("ella", "она", "she");
        basic.add("nosotros", "мы", "we");
        basic.add("ellos", "они", "they");
        basic.add("esto", "это", "this");
        basic.add("eso", "то", "that");
        list.add(basic);

        StarterFolder numbers = new StarterFolder("Числа", "Numbers");
        numbers.add("uno", "один", "one");
        numbers.add("dos", "два", "two");
        numbers.add("tres", "три", "three");
        numbers.add("cuatro", "четыре", "four");
        numbers.add("cinco", "пять", "five");
        numbers.add("seis", "шесть", "six");
        numbers.add("siete", "семь", "seven");
        numbers.add("ocho", "восемь", "eight");
        numbers.add("nueve", "девять", "nine");
        numbers.add("diez", "десять", "ten");
        list.add(numbers);

        StarterFolder family = new StarterFolder("Семья", "Family");
        family.add("madre", "мама", "mother");
        family.add("padre", "папа", "father");
        family.add("hermano", "брат", "brother");
        family.add("hermana", "сестра", "sister");
        family.add("hijo", "сын", "son");
        family.add("hija", "дочь", "daughter");
        family.add("amigo", "друг", "friend");
        list.add(family);

        StarterFolder food = new StarterFolder("Еда", "Food");
        food.add("agua", "вода", "water");
        food.add("pan", "хлеб", "bread");
        food.add("leche", "молоко", "milk");
        food.add("manzana", "яблоко", "apple");
        food.add("té", "чай", "tea");
        food.add("café", "кофе", "coffee");
        list.add(food);

        return list;
    }
}