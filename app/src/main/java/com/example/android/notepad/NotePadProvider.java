package com.example.android.notepad;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Objects;

public class NotePadProvider extends ContentProvider {
    private static final String DATABASE_NAME = "notes.db";
    // 版本从 3 升级到 4：新增 back_color 字段
    private static final int DATABASE_VERSION = 4;
    private static final String NOTES_TABLE_NAME = "notes";

    // URI匹配器（区分单条/多条数据）
    private static final int NOTES = 1;
    private static final int NOTE_ID = 2;
    private static final UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);
    }

    // 数据库帮助类
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // 创建表（包含颜色字段）
            db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_NOTE_TYPE + " INTEGER," // <--- 新增这一行
                    + NotePad.Notes.COLUMN_NAME_BACK_COLOR + " INTEGER"
                    + ");");

            // 插入一些示例数据
            db.execSQL("INSERT INTO " + NOTES_TABLE_NAME + " ("
                    + NotePad.Notes.COLUMN_NAME_TITLE + ", "
                    + NotePad.Notes.COLUMN_NAME_NOTE + ", "
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + ", "
                    + NotePad.Notes.COLUMN_NAME_BACK_COLOR
                    + ") VALUES ('欢迎使用记事本', '这是一个示例笔记', strftime('%s', 'now') * 1000, 0)");
            db.execSQL("INSERT INTO " + NOTES_TABLE_NAME + " ("
                    + NotePad.Notes.COLUMN_NAME_TITLE + ", "
                    + NotePad.Notes.COLUMN_NAME_NOTE + ", "
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + ", "
                    + NotePad.Notes.COLUMN_NAME_BACK_COLOR
                    + ") VALUES ('购物清单', '牛奶, 鸡蛋, 面包', strftime('%s', 'now') * 1000, 1)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 版本升级策略
            for (int version = oldVersion + 1; version <= newVersion; version++) {
                switch (version) {
                    case 2:
                        // 从版本1升级到版本2：添加颜色字段
                        db.execSQL("ALTER TABLE " + NOTES_TABLE_NAME + " ADD COLUMN "
                                + NotePad.Notes.COLUMN_NAME_BACK_COLOR + " INTEGER DEFAULT 0");
                        break;
                    case 3:
                        // 从版本2升级到版本3：添加create_date字段
                        db.execSQL("ALTER TABLE " + NOTES_TABLE_NAME + " ADD COLUMN "
                                + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER DEFAULT (strftime('%s', 'now') * 1000)");
                        // 为现有数据设置创建时间（使用修改时间作为创建时间）
                        db.execSQL("UPDATE " + NOTES_TABLE_NAME + " SET "
                                + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " = "
                                + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
                                + " WHERE " + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " IS NULL");
                        break;
                    case 4:
                        // 版本4：确保所有字段都有正确的默认值
                        // 确保create_date有值
                        db.execSQL("UPDATE " + NOTES_TABLE_NAME + " SET "
                                + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " = (strftime('%s', 'now') * 1000)"
                                + " WHERE " + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " IS NULL");
                        // 确保back_color有值
                        db.execSQL("UPDATE " + NOTES_TABLE_NAME + " SET "
                                + NotePad.Notes.COLUMN_NAME_BACK_COLOR + " = 0"
                                + " WHERE " + NotePad.Notes.COLUMN_NAME_BACK_COLOR + " IS NULL");
                        break;
                }
            }
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        String orderBy = TextUtils.isEmpty(sortOrder) ? NotePad.Notes.DEFAULT_SORT_ORDER : sortOrder;

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                // 查询所有笔记
                return db.query(NOTES_TABLE_NAME, projection, selection, selectionArgs,
                        null, null, orderBy);
            case NOTE_ID:
                // 查询单条笔记（按ID）
                String noteId = uri.getPathSegments().get(1);
                String where = NotePad.Notes._ID + "=" + noteId
                        + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")");
                return db.query(NOTES_TABLE_NAME, projection, where, selectionArgs,
                        null, null, orderBy);
            default:
                throw new IllegalArgumentException("未知URI: " + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                return NotePad.Notes.CONTENT_TYPE;
            case NOTE_ID:
                return NotePad.Notes.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("未知URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("未知URI: " + uri);
        }

        // 补全默认值
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE)) {
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, "未命名笔记");
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE)) {
            values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, System.currentTimeMillis());
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        }
        // 颜色字段：默认值为0（白色）
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_BACK_COLOR)) {
            values.put(NotePad.Notes.COLUMN_NAME_BACK_COLOR, 0);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(NOTES_TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, rowId);
            Objects.requireNonNull(getContext()).getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }
        throw new RuntimeException("插入数据失败: " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.delete(NOTES_TABLE_NAME, selection, selectionArgs);
                break;
            case NOTE_ID:
                String noteId = uri.getPathSegments().get(1);
                String where = NotePad.Notes._ID + "=" + noteId
                        + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")");
                count = db.delete(NOTES_TABLE_NAME, where, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("未知URI: " + uri);
        }
        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;

        // 更新时自动刷新修改时间（不修改create_date，保持创建时间不变）
        // 注意：只有在实际更新时才设置修改时间
        boolean hasRealChanges = false;
        for (String key : values.keySet()) {
            if (!key.equals(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
                hasRealChanges = true;
                break;
            }
        }
        if (hasRealChanges) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        }

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.update(NOTES_TABLE_NAME, values, selection, selectionArgs);
                break;
            case NOTE_ID:
                String noteId = uri.getPathSegments().get(1);
                String where = NotePad.Notes._ID + "=" + noteId
                        + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")");
                count = db.update(NOTES_TABLE_NAME, values, where, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("未知URI: " + uri);
        }
        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
        return count;
    }
}