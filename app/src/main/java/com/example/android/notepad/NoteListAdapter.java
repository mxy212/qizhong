package com.example.android.notepad;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class NoteListAdapter extends CursorAdapter {

    public NoteListAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.note_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // 1. 绑定标题
        int titleColumnIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
        TextView titleTv = view.findViewById(R.id.note_title); // 使用 R.id.note_title

        if (titleColumnIndex != -1) {
            String title = cursor.getString(titleColumnIndex);
            titleTv.setText(title == null ? "无标题" : title);
        } else {
            titleTv.setText("无标题");
        }

        // 2. 绑定时间戳 - 修正为正确的ID
        // 选项1：如果布局中使用的是 note_time
        TextView timestampTv = view.findViewById(R.id.note_time);

        // 选项2：如果布局中使用的是 text1_time
        // TextView timestampTv = view.findViewById(R.id.text1_time);

        // 获取时间列（使用修改时间或创建时间）
        int timeColumnIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);
        if (timeColumnIndex == -1) {
            timeColumnIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CREATE_DATE);
        }

        if (timeColumnIndex != -1) {
            long time = cursor.getLong(timeColumnIndex);
            // 格式化时间显示
            String timeStr = android.text.format.DateUtils.formatDateTime(
                    context,
                    time,
                    android.text.format.DateUtils.FORMAT_SHOW_DATE | android.text.format.DateUtils.FORMAT_SHOW_TIME
            );
            timestampTv.setText("最后修改：" + timeStr);
        } else {
            timestampTv.setText("最后修改：未知时间");
        }
    }
}