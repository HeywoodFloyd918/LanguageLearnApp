package com.example.learn_dict_cards_languages_mila.ui.training;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AlertDialog;

import com.example.learn_dict_cards_languages_mila.R;
import com.example.learn_dict_cards_languages_mila.data.Folder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FolderSelectionDialog {

    public interface OnFoldersSelected {
        void onSelected(boolean selectAll, Set<Integer> folderIds);
    }

    private final Context context;

    public FolderSelectionDialog(Context context) {
        this.context = context;
    }

    public void show(List<Folder> folders,
                     boolean initialSelectAll,
                     Set<Integer> initialSelected,
                     OnFoldersSelected callback) {

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * context.getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, pad / 2);

        final CheckBox cbAll = new CheckBox(context);
        cbAll.setText(R.string.folders_all);
        cbAll.setChecked(initialSelectAll);
        container.addView(cbAll);

        final List<CheckBox> folderBoxes = new ArrayList<>();
        for (Folder f : folders) {
            CheckBox cb = new CheckBox(context);
            cb.setText(f.name);
            cb.setChecked(initialSelectAll || initialSelected.contains(f.id));
            container.addView(cb);
            folderBoxes.add(cb);
        }

        final boolean[] isUpdating = {false};

        cbAll.setOnCheckedChangeListener((v, checked) -> {
            if (isUpdating[0]) return;
            isUpdating[0] = true;
            for (CheckBox cb : folderBoxes) cb.setChecked(checked);
            isUpdating[0] = false;
        });

        for (CheckBox cb : folderBoxes) {
            cb.setOnCheckedChangeListener((v, checked) -> {
                if (isUpdating[0]) return;
                isUpdating[0] = true;
                boolean allChecked = true;
                for (CheckBox c : folderBoxes) {
                    if (!c.isChecked()) { allChecked = false; break; }
                }
                cbAll.setChecked(allChecked);
                isUpdating[0] = false;
            });
        }

        ScrollView scroll = new ScrollView(context);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.addView(container);

        new AlertDialog.Builder(context)
                .setTitle(R.string.select_folders_title)
                .setView(scroll)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    boolean selectAll = cbAll.isChecked();
                    Set<Integer> selected = new HashSet<>();
                    for (int i = 0; i < folderBoxes.size(); i++) {
                        if (folderBoxes.get(i).isChecked()) selected.add(folders.get(i).id);
                    }
                    if (!folders.isEmpty() && selected.size() == folders.size()) {
                        selectAll = true;
                    }
                    callback.onSelected(selectAll, selected);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}