package com.example.android.notepad;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * This Activity allows the user to edit a note's title. It displays a floating window
 * containing an EditText.
 */
public class TitleEditor extends Activity {

    public static final String EDIT_TITLE_ACTION = "com.android.notepad.action.EDIT_TITLE";

    // 创建投影，返回笔记ID和标题
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
    };

    // 标题列在Cursor中的位置
    private static final int COLUMN_INDEX_TITLE = 1;

    private Cursor mCursor;
    private EditText mText;
    private Uri mUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.title_editor); // 需要title_editor.xml布局文件

        mUri = getIntent().getData();

        if (mUri != null) {
            // 查询笔记
            mCursor = getContentResolver().query(
                    mUri,
                    PROJECTION,
                    null,
                    null,
                    null
            );
        }

        mText = findViewById(R.id.title);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCursor != null && mCursor.moveToFirst()) {
            // 显示当前标题
            mText.setText(mCursor.getString(COLUMN_INDEX_TITLE));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCursor != null && mText != null) {
            // 更新标题到数据库
            ContentValues values = new ContentValues();
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, mText.getText().toString());

            getContentResolver().update(
                    mUri,
                    values,
                    null,
                    null
            );
        }
    }

    public void onClickOk(View v) {
        finish();
    }
}