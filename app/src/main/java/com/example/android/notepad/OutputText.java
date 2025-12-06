package com.example.android.notepad;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class OutputText extends Activity {
    // 读取笔记的标题、内容、创建时间、修改时间
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_NOTE, // 2
            NotePad.Notes.COLUMN_NAME_CREATE_DATE, // 3
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 4
    };
    private String TITLE, NOTE, CREATE_DATE, MODIFICATION_DATE;
    private Cursor mCursor;
    private EditText mName;
    private Uri mUri;
    private boolean flag = false; // 标记是否点击导出按钮

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.output_text);
        // 获取笔记URI
        mUri = getIntent().getData();
        // 查询笔记信息
        mCursor = managedQuery(mUri, PROJECTION, null, null, null);
        // 绑定文件名输入框
        mName = (EditText) findViewById(R.id.output_name);

    }

    @Override
    protected void onResume(){
        super.onResume();
        if (mCursor != null) {
            mCursor.moveToFirst();
            // 默认文件名=笔记标题
            mName.setText(mCursor.getString(1));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCursor != null && flag) {
            // 读取笔记信息
            TITLE = mCursor.getString(1);
            NOTE = mCursor.getString(2);
            CREATE_DATE = mCursor.getString(3);
            MODIFICATION_DATE = mCursor.getString(4);
            // 写入文件
            write();
            flag = false;
        }
    }

    // 点击OK按钮触发
    public void OutputOk(View v){
        flag = true;
        finish();
    }

    // 写入SD卡逻辑
    private void write() {
        try {
            // 检查SD卡是否挂载
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File sdCardDir = Environment.getExternalStorageDirectory();
                // 创建文件（UTF-8编码）
                File targetFile = new File(sdCardDir.getCanonicalPath() + "/" + mName.getText() + ".txt");
                PrintWriter ps = new PrintWriter(new OutputStreamWriter(new FileOutputStream(targetFile), "UTF-8"));
                // 写入内容
                ps.println(TITLE);
                ps.println(NOTE);
                ps.println("创建时间：" + CREATE_DATE);
                ps.println("最后一次修改时间：" + MODIFICATION_DATE);
                ps.close();
                // 提示保存成功
                Toast.makeText(this, "保存成功：" + sdCardDir.getCanonicalPath() + "/" + mName.getText() + ".txt", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }
}