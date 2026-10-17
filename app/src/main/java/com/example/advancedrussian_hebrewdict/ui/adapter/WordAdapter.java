package com.example.advancedrussian_hebrewdict.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.advancedrussian_hebrewdict.R;
import com.example.advancedrussian_hebrewdict.data.Word;

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

    public WordAdapter(List<Word> words, OnItemClickListener listener) {
        this(words, listener, null);
    }

    public WordAdapter(List<Word> words, OnItemClickListener listener,
                       OnSpeakClickListener speakListener) {
        this.words = words;
        this.listener = listener;
        this.speakListener = speakListener;
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

        // Иконка динамика — показывается только если включено в настройках
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

        // Клик по карточке
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

    /** Перемещение элемента для drag&drop. */
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
        TextView russianText, hebrewText, commentText, genderText, learnedText;
        ImageButton speakButton;

        WordViewHolder(@NonNull View itemView) {
            super(itemView);
            russianText = itemView.findViewById(R.id.tv_russian);
            hebrewText = itemView.findViewById(R.id.tv_hebrew);
            commentText = itemView.findViewById(R.id.tv_comment);
            genderText = itemView.findViewById(R.id.tv_gender);
            learnedText = itemView.findViewById(R.id.tv_learned);
            speakButton = itemView.findViewById(R.id.btn_speak_card);
        }
    }
}