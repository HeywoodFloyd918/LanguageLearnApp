package com.example.learn_dict_cards_languages_mila.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.learn_dict_cards_languages_mila.R;
import com.example.learn_dict_cards_languages_mila.data.Word;

import java.util.Collections;
import java.util.List;

public class WordAdapter extends RecyclerView.Adapter<WordAdapter.WordViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Word word);
    }

    public interface OnSpeakClickListener {
        void onSpeakClick(Word word);
    }

    private List<Word> words;
    private final OnItemClickListener listener;
    private final OnSpeakClickListener speakListener;
    private final boolean isSearchMode; // Новый флаг

    // Конструктор для совместимости (по умолчанию не поиск)
    public WordAdapter(List<Word> words, OnItemClickListener listener) {
        this(words, listener, null, false);
    }

    public WordAdapter(List<Word> words, OnItemClickListener listener,
                       OnSpeakClickListener speakListener) {
        this(words, listener, speakListener, false);
    }

    public WordAdapter(List<Word> words, OnItemClickListener listener,
                       OnSpeakClickListener speakListener, boolean isSearchMode) {
        this.words = words;
        this.listener = listener;
        this.speakListener = speakListener;
        this.isSearchMode = isSearchMode;
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_word, parent, false);
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        Word word = words.get(position);

        holder.russianText.setText(word.russian);
        holder.hebrewText.setText(word.hebrew);

        // Логика окрашивания (только если это режим поиска)
        if (isSearchMode) {
            boolean isTopResult = (position == 0);
            boolean isHighlyProbableSecond = (position == 1 && word.probability >= 0.85f);

            if (isTopResult || isHighlyProbableSecond) {
                // Нежно-зеленый цвет (Material Green 50)
                holder.cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            } else {
                // Стандартный белый цвет
                holder.cardView.setCardBackgroundColor(Color.WHITE);
            }
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
        }

        // Род
        if (word.gender != null && !word.gender.isEmpty()) {
            holder.genderText.setText(word.gender);
            holder.genderText.setVisibility(View.VISIBLE);
        } else {
            holder.genderText.setVisibility(View.GONE);
        }

        // Комментарий
        if (word.comment != null && !word.comment.isEmpty()) {
            holder.commentText.setText(word.comment);
            holder.commentText.setVisibility(View.VISIBLE);
        } else {
            holder.commentText.setVisibility(View.GONE);
        }

        // Метка «выучено»
        if (word.isLearned && word.id > 0) {
            holder.learnedText.setVisibility(View.VISIBLE);
        } else {
            holder.learnedText.setVisibility(View.GONE);
        }

        // Иконка динамика
        Context ctx = holder.itemView.getContext();
        SharedPreferences prefs = ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean speakOnCard = prefs.getBoolean("speak_on_card", true);

        if (speakOnCard && word.hebrew != null && !word.hebrew.isEmpty()) {
            holder.speakButton.setVisibility(View.VISIBLE);
            holder.speakButton.setOnClickListener(v -> {
                if (speakListener != null) speakListener.onSpeakClick(word);
            });
        } else {
            holder.speakButton.setVisibility(View.GONE);
            holder.speakButton.setOnClickListener(null);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(word);
        });
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    public void updateList(List<Word> newWords) {
        this.words = newWords;
        notifyDataSetChanged();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(words, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(words, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    public List<Word> getWords() {
        return words;
    }

    static class WordViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView russianText, hebrewText, commentText, genderText, learnedText;
        ImageButton speakButton;

        WordViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            russianText = itemView.findViewById(R.id.tv_russian);
            hebrewText = itemView.findViewById(R.id.tv_hebrew);
            commentText = itemView.findViewById(R.id.tv_comment);
            genderText = itemView.findViewById(R.id.tv_gender);
            learnedText = itemView.findViewById(R.id.tv_learned);
            speakButton = itemView.findViewById(R.id.btn_speak_card);
        }
    }
}