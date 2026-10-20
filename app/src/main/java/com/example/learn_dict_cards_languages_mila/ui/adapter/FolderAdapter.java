package com.example.learn_dict_cards_languages_mila.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.learn_dict_cards_languages_mila.R;
import com.example.learn_dict_cards_languages_mila.data.FolderWithCount;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public interface OnFolderClickListener {
        void onFolderClick(FolderWithCount folder);
    }

    public interface OnFolderLongClickListener {
        void onFolderLongClick(FolderWithCount folder);
    }

    private List<FolderWithCount> folders;
    private final OnFolderClickListener listener;
    private final OnFolderLongClickListener longClickListener;

    public FolderAdapter(List<FolderWithCount> folders,
                         OnFolderClickListener listener,
                         OnFolderLongClickListener longClickListener) {
        this.folders = folders;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        FolderWithCount folder = folders.get(position);
        Context ctx = holder.itemView.getContext();

        holder.nameText.setText(folder.name);
        holder.countText.setText(ctx.getString(R.string.words_count, folder.wordCount));

        if (folder.updatedAt > 0) {
            String formatted = ctx.getString(R.string.changed_at,
                    DATE_FMT.format(new Date(folder.updatedAt)));
            holder.dateText.setText(formatted);
            holder.dateText.setVisibility(View.VISIBLE);
        } else {
            holder.dateText.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFolderClick(folder);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onFolderLongClick(folder);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    public void updateList(List<FolderWithCount> newFolders) {
        this.folders = newFolders;
        notifyDataSetChanged();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, countText, dateText;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_folder_name);
            countText = itemView.findViewById(R.id.tv_folder_count);
            dateText = itemView.findViewById(R.id.tv_folder_date);
        }
    }
}