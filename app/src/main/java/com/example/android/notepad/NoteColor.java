package com.example.android.notepad;

// 补充所有必要的导入语句（核心修复）
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class NoteColor extends Activity {
    private Cursor mCursor;
    private Uri mUri;
    private int color;

    // 投影：仅查询ID和背景颜色字段（精简查询）
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,          // 索引0
            NotePad.Notes.COLUMN_NAME_BACK_COLOR  // 索引1
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_color);

        // 获取从NoteEditor传入的笔记URI（增加空值判断）
        mUri = getIntent().getData();
        if (mUri == null) {
            // 无有效URI时直接结束，避免空指针
            finish();
            return;
        }

        // 修复：替换过时的managedQuery（API 11+ 废弃），改用getContentResolver.query
        mCursor = getContentResolver().query(
                mUri,
                PROJECTION,
                null,
                null,
                null
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 增加空指针和游标有效性判断（核心修复）
        if (mCursor != null && !mCursor.isClosed() && mCursor.moveToFirst()) {
            // 读取颜色值：使用常量索引，避免魔法数
            int colorColumnIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_BACK_COLOR);
            if (colorColumnIndex != -1) {
                color = mCursor.getInt(colorColumnIndex);
            } else {
                // 字段不存在时使用默认颜色
                color = NotePad.Notes.WHITE_COLOR;
            }
        } else {
            // 游标无效时使用默认颜色
            color = NotePad.Notes.WHITE_COLOR;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 增加URI和游标有效性判断，避免空指针更新
        if (mUri != null) {
            // 将选择的颜色更新到数据库
            ContentValues values = new ContentValues();
            values.put(NotePad.Notes.COLUMN_NAME_BACK_COLOR, color);
            // 执行更新操作
            getContentResolver().update(mUri, values, null, null);
        }

        // 关闭游标，释放资源（核心修复：避免内存泄漏）
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
            mCursor = null;
        }
    }

    // 颜色点击事件：选择后更新color并结束Activity
    public void white(View view) {
        color = NotePad.Notes.WHITE_COLOR;
        finish();
    }

    public void yellow(View view) {
        color = NotePad.Notes.YELLOW_COLOR;
        finish();
    }

    public void blue(View view) {
        color = NotePad.Notes.BLUE_COLOR;
        finish();
    }

    public void green(View view) {
        color = NotePad.Notes.GREEN_COLOR;
        finish();
    }

    public void red(View view) {
        color = NotePad.Notes.RED_COLOR;
        finish();
    }
}