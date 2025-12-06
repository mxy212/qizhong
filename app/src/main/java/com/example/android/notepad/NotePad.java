package com.example.android.notepad;

import android.net.Uri;
import android.provider.BaseColumns;
import android.graphics.Color;

public final class NotePad {
    private NotePad() {}

    public static final String AUTHORITY = "com.example.android.notepad.provider";
    public static final String COLUMN_NAME_NOTE_TYPE = "note_type";
        public static final class Notes implements BaseColumns {
        private Notes() {}

        // 表名
        public static final String TABLE_NAME = "notes";

        // 字段定义
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_NOTE = "note";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_CREATE_DATE = "create_date";
        public static final String COLUMN_NAME_MODIFICATION_DATE = "modified_date";
        public static final String COLUMN_NAME_BACK_COLOR = "back_color";

        // 颜色常量
        public static final int WHITE_COLOR = 0; // 默认白色
        public static final int YELLOW_COLOR = Color.YELLOW;
        public static final int BLUE_COLOR = Color.BLUE;
        public static final int GREEN_COLOR = Color.GREEN;
        public static final int RED_COLOR = Color.RED;

        public static final String COLUMN_NAME_NOTE_TYPE = "note_type";
        // URI 定义
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/notes");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.example.note";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.example.note";
        public static final Uri CONTENT_ID_URI_BASE =
                Uri.parse("content://" + AUTHORITY + "/notes/");
        // 默认排序
        public static final String DEFAULT_SORT_ORDER = COLUMN_NAME_MODIFICATION_DATE + " DESC";
        public static final Uri LIVE_FOLDER_URI = Uri.parse("content://" + AUTHORITY + "/live_folders/notes");
        public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse("content://" + AUTHORITY + "/notes/#");

    }
}