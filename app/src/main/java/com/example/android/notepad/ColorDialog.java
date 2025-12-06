package com.example.android.notepad;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class ColorDialog extends DialogFragment {

    // 颜色选择回调接口
    public interface ColorSelectionListener {
        void onColorSelected(int color);
    }

    private ColorSelectionListener mListener;

    // 设置监听器
    public void setColorSelectionListener(ColorSelectionListener listener) {
        mListener = listener;
    }

    // 预设颜色（白色、浅红、浅绿、浅蓝、浅黄、浅紫、浅灰）
    private final int[] colors = {
            0xFFFFFFFF, 0xFFFFCDD2, 0xFFC8E6C9, 0xFFBBDEFB,
            0xFFFFF9C4, 0xFFE1BEE7, 0xFFF5F5F5
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setTitle("选择背景颜色");

        // 创建网格布局（3列）
        GridLayout grid = new GridLayout(requireContext());
        grid.setColumnCount(3);
        grid.setPadding(20, 20, 20, 20);

        // 添加颜色选项
        for (int color : colors) {
            View colorView = new View(requireContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 100;
            params.height = 100;
            params.setMargins(10, 10, 10, 10);
            colorView.setLayoutParams(params);
            colorView.setBackgroundColor(color);
            colorView.setClickable(true);
            // 点击事件：返回选择的颜色
            colorView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onColorSelected(color);
                }
                dismiss();
            });
            grid.addView(colorView);
        }

        dialog.setContentView(grid);
        return dialog;
    }
}