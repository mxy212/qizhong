package com.example.android.notepad;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class NoteDbHelper extends SQLiteOpenHelper {
    // 数据库版本升级：从 2 → 3（新增颜色字段需升级版本）
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "NotePad.db";


    // 笔记表建表语句（新增 BACK_COLOR 颜色字段，同时保留原有字段）
    private static final String SQL_CREATE_NOTES_TABLE =
            "CREATE TABLE " + NotePad.Notes.TABLE_NAME + " (" +
                    BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    NotePad.Notes.COLUMN_NAME_TITLE + " TEXT," +
                    NotePad.Notes.COLUMN_NAME_NOTE + " TEXT," +
                    NotePad.Notes.COLUMN_NAME_TIMESTAMP + " TEXT DEFAULT CURRENT_TIMESTAMP," + // 原有新增字段
                    NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER," + // 补全原有日期字段
                    NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER," + // 补全修改日期字段
                    NotePad.Notes.COLUMN_NAME_BACK_COLOR + " INTEGER" + // 新增颜色字段（核心修改）
                    ");";

    // 表删除语句
    private static final String SQL_DELETE_NOTES_TABLE =
            "DROP TABLE IF EXISTS " + NotePad.Notes.TABLE_NAME;

    public NoteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 执行建表语句（包含颜色字段）
        db.execSQL(SQL_CREATE_NOTES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 版本升级逻辑：覆盖旧版本 → 新版本的升级场景
        if (oldVersion < 2) {
            // 从版本1升级到2：处理timestamp字段
            db.execSQL(SQL_DELETE_NOTES_TABLE);
            onCreate(db);
        }
        if (oldVersion < 3) {
            // 从版本2升级到3：处理back_color字段（核心新增）
            db.execSQL(SQL_DELETE_NOTES_TABLE);
            onCreate(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}