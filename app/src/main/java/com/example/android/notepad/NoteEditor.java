package com.example.android.notepad;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteEditor extends AppCompatActivity {
    private EditText mTitleEdit, mContentEdit;
    private Uri mNoteUri; // 当前笔记URI（编辑时非空）
    private String mOriginalTitle, mOriginalContent; // 原始内容（用于撤回）
    private int mSelectedColorCode; // 存储颜色常量值
    private int mOriginalColorCode; // 原始颜色常量值
    private boolean mIsContentChanged = false; // 标记内容是否被修改

    // 定义分类名称
    private static final String[] CATEGORIES = {"无分类", "工作", "生活", "学习"};
    // 声明 Spinner
    private Spinner mSpinner;

    // 定义颜色数据
    private final String[] COLOR_NAMES = {"白色", "黄色", "淡蓝", "粉色", "淡绿"};
    private final int[] COLOR_VALUES = {
            0xFFFFFFFF, // 白色
            0xFFFFFFE0, // 黄色
            0xFFE0FFFF, // 淡蓝
            0xFFFFC0CB, // 粉色
            0xFF90EE90  // 淡绿
    };

    // 权限相关（Android 10+不需要WRITE_EXTERNAL_STORAGE）
    private static final int WRITE_PERMISSION_REQUEST = 1001;
    private static final String[] REQUIRED_PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // 实时输入相关
    private Editable mContentEditable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. 主题设置
        SharedPreferences sp = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = sp.getBoolean("dark_mode", false);
        setTheme(isDarkMode ? R.style.AppTheme_Dark : R.style.AppTheme_Light);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_editor);

        // 2. 绑定控件
        mTitleEdit = findViewById(R.id.note_title);
        mContentEdit = findViewById(R.id.note_content);

        // =========== 【关键修改】绑定并初始化 Spinner ===========
        mSpinner = findViewById(R.id.spinner_note_type);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                CATEGORIES
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        // ======================================================

        // 3. 监听器初始化
        initContentListener();
        setContentChangeListeners();

        // 4. 处理数据加载
        mNoteUri = getIntent().getData();
        if (mNoteUri != null) {
            // 编辑模式：加载已有数据（包括分类）
            loadNoteData();
        } else {
            // 新建模式：设置默认值
            mTitleEdit.setText(getString(R.string.untitled_note));

            mOriginalColorCode = 0;
            mSelectedColorCode = mOriginalColorCode;
            mContentEdit.setBackgroundColor(getColorFromCode(mSelectedColorCode));

            mOriginalTitle = getString(R.string.untitled_note);
            mOriginalContent = "";

            // 新建笔记默认选中第0项（无分类）
            mSpinner.setSelection(0);
        }
    }

    // 初始化内容监听
    private void initContentListener() {
        mContentEditable = mContentEdit.getText();
    }

    // 监听标题和内容变化，标记是否修改
    private void setContentChangeListeners() {
        mTitleEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) { mIsContentChanged = true; }
        });

        mContentEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) { mIsContentChanged = true; }
        });
    }

    // 从数据库加载笔记数据
    private void loadNoteData() {
        // 【关键修改】修复了 NotePad.Notes.COLUMN_NAME_NOTE_TYPE 前面的空格
        String[] projection = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_NOTE,
                NotePad.Notes.COLUMN_NAME_BACK_COLOR,
                NotePad.Notes.COLUMN_NAME_CREATE_DATE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_NOTE_TYPE // 这里要对应数据库列名
        };

        Cursor cursor = getContentResolver().query(mNoteUri, projection, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            mOriginalTitle = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_TITLE));
            mOriginalContent = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_NOTE));

            // 获取颜色值
            int colorColumnIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_BACK_COLOR);
            if (colorColumnIndex >= 0) {
                mOriginalColorCode = cursor.getInt(colorColumnIndex);
            } else {
                mOriginalColorCode = 0; // 默认白色
            }
            mSelectedColorCode = mOriginalColorCode;

            // =========== 【关键修改】读取分类并设置 ===========
            int typeIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE_TYPE);
            if (typeIndex > -1) {
                int savedType = cursor.getInt(typeIndex);
                // 防止越界
                if (savedType >= 0 && savedType < CATEGORIES.length) {
                    mSpinner.setSelection(savedType);
                }
            }
            // ==================================================

            // 显示数据
            mTitleEdit.setText(mOriginalTitle);
            mContentEdit.setText(mOriginalContent);
            mContentEdit.setBackgroundColor(getColorFromCode(mSelectedColorCode));

            cursor.close();
        } else {
            Toast.makeText(this, getString(R.string.load_failed), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // 时间格式化工具
    private String formatTime(long timeStamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timeStamp));
    }

    // 保存笔记到数据库
    private void saveNote() {
        // 如果内容没变且是编辑模式，可以直接返回。但如果是新建，且Spinner改了，也应该保存。
        // 这里简单处理：只要是保存操作，就更新所有数据

        String title = mTitleEdit.getText().toString().trim();
        String content = mContentEdit.getText().toString().trim();
        if (TextUtils.isEmpty(title)) title = getString(R.string.untitled_note);

        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, content);
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        values.put(NotePad.Notes.COLUMN_NAME_BACK_COLOR, mSelectedColorCode);

        // =========== 【关键修改】保存分类数据 ===========
        if (mSpinner != null) {
            values.put(NotePad.Notes.COLUMN_NAME_NOTE_TYPE, mSpinner.getSelectedItemPosition());
        }
        // ==================================================

        if (mNoteUri == null) {
            // 新建笔记：设置创建时间
            values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, System.currentTimeMillis());
            Uri newUri = getContentResolver().insert(NotePad.Notes.CONTENT_URI, values);
            if (newUri == null) {
                Toast.makeText(this, getString(R.string.save_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show();
        } else {
            // 编辑笔记：更新数据
            int rows = getContentResolver().update(mNoteUri, values, null, null);
            if (rows <= 0) {
                // 有时候没改动update返回0是正常的，不一定要报错
            } else {
                Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show();
            }
        }
        mIsContentChanged = false;
        finish(); // 返回列表页
    }

    // 删除当前笔记
    private void deleteNote() {
        if (mNoteUri == null) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_delete))
                .setMessage(getString(R.string.delete_message))
                .setPositiveButton(getString(R.string.menu_delete), (dialog, which) -> {
                    getContentResolver().delete(mNoteUri, null, null);
                    Toast.makeText(this, getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    // 撤销/恢复到原始内容
    private void revertToOriginal() {
        if (mNoteUri != null) {
            mTitleEdit.setText(mOriginalTitle);
            mContentEdit.setText(mOriginalContent);
            mSelectedColorCode = mOriginalColorCode;
            mContentEdit.setBackgroundColor(getColorFromCode(mSelectedColorCode));

            // 恢复分类选择
            // 这里比较复杂，因为没有保存原始分类。通常需要重新查询数据库。
            // 简单做法：重新调用 loadNoteData()
            loadNoteData();

            mIsContentChanged = false;
            Toast.makeText(this, getString(R.string.revert_success), Toast.LENGTH_SHORT).show();
        } else {
            mTitleEdit.setText(getString(R.string.untitled_note));
            mContentEdit.setText("");
            mSelectedColorCode = 0;
            mContentEdit.setBackgroundColor(getColorFromCode(mSelectedColorCode));
            mSpinner.setSelection(0); // 恢复默认分类
            mIsContentChanged = false;
            Toast.makeText(this, getString(R.string.clear_content), Toast.LENGTH_SHORT).show();
        }
    }

    // 颜色代码转换为实际颜色值
    private int getColorFromCode(int colorCode) {
        if (colorCode < 0 || colorCode >= COLOR_VALUES.length) {
            colorCode = 0;
        }
        return COLOR_VALUES[colorCode];
    }

    // 保存颜色到数据库
    private void saveColorToDatabase(int colorIndex) {
        if (mNoteUri == null) return;
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_BACK_COLOR, colorIndex);
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        getContentResolver().update(mNoteUri, values, null, null);
    }

    // 导出笔记分享
    private void exportNote() {
        String content = mContentEdit.getText().toString();
        String title = mTitleEdit.getText().toString();
        if (TextUtils.isEmpty(content) && TextUtils.isEmpty(title)) {
            Toast.makeText(this, "没有内容可以分享", Toast.LENGTH_SHORT).show();
            return;
        }
        String shareText = "标题: " + title + "\n\n" + content;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        intent.putExtra(Intent.EXTRA_TITLE, "笔记分享");
        startActivity(Intent.createChooser(intent, "分享笔记到..."));
    }

    // 更改背景颜色
    private void changeBackgroundColor() {
        new AlertDialog.Builder(NoteEditor.this)
                .setTitle("选择背景颜色")
                .setItems(COLOR_NAMES, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mContentEdit != null) {
                            mContentEdit.setBackgroundColor(COLOR_VALUES[which]);
                            mSelectedColorCode = which;
                            // 如果是编辑已有笔记，直接保存颜色
                            saveColorToDatabase(which);
                        }
                    }
                })
                .show();
    }

    // 撤回逻辑
    private void cancelNote() {
        if (mNoteUri != null) {
            revertToOriginal();
        } else {
            if (mIsContentChanged) {
                new AlertDialog.Builder(this)
                        .setTitle("提示")
                        .setMessage("是否放弃修改？")
                        .setPositiveButton("放弃", (dialog, which) -> finish())
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                finish();
            }
        }
    }

    // 导出笔记为TXT文件
    private void exportNoteToTxt() {
        String title = mTitleEdit.getText().toString().trim();
        String content = mContentEdit.getText().toString().trim();
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content)) {
            Toast.makeText(this, getString(R.string.no_content), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File dir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Notepad");
            } else {
                dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Notepad");
            }
            if (!dir.exists() && !dir.mkdirs()) {
                Toast.makeText(this, getString(R.string.create_dir_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            String safeTitle = TextUtils.isEmpty(title) ? getString(R.string.untitled_note) : title;
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = safeTitle.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_") + "_" + timeStamp + ".txt";
            File file = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                osw.write("=== 笔记导出 ===\n");
                osw.write("标题：" + safeTitle + "\n");
                osw.write("导出时间：" + formatTime(System.currentTimeMillis()) + "\n");
                // 写入分类信息
                String category = CATEGORIES[mSpinner.getSelectedItemPosition()];
                osw.write("分类：" + category + "\n");

                osw.write("\n=== 笔记内容 ===\n");
                osw.write(TextUtils.isEmpty(content) ? "无内容" : content);
                osw.flush();
            }
            String message = "导出成功：\n" + file.getAbsolutePath();
            new AlertDialog.Builder(this)
                    .setTitle("导出成功")
                    .setMessage(message)
                    .setPositiveButton("打开文件", (dialog, which) -> openExportedFile(file))
                    .setNegativeButton("确定", null)
                    .show();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(file));
                sendBroadcast(mediaScanIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private long getNoteCreateTime() { return System.currentTimeMillis(); } // 简化版
    private long getNoteModifyTime() { return System.currentTimeMillis(); } // 简化版

    private void openExportedFile(File file) {
        Uri fileUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        } else {
            fileUri = Uri.fromFile(file);
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "text/plain");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(intent); } catch (Exception e) { Toast.makeText(this, "没有找到文本编辑器", Toast.LENGTH_SHORT).show(); }
    }

    private boolean needStoragePermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, REQUIRED_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_save) {
            saveNote();
            return true;
        } else if (itemId == R.id.menu_delete) {
            deleteNote();
            return true;
        } else if (itemId == R.id.menu_revert) {
            cancelNote();
            return true;
        } else if (itemId == R.id.menu_color) {
            changeBackgroundColor();
            return true;
        } else if (itemId == R.id.menu_export) {
            new AlertDialog.Builder(this)
                    .setTitle("选择导出方式")
                    .setItems(new String[]{"分享笔记", "保存为TXT文件"}, (dialog, which) -> {
                        if (which == 0) exportNote();
                        else {
                            if (needStoragePermission()) ActivityCompat.requestPermissions(NoteEditor.this, REQUIRED_PERMISSIONS, WRITE_PERMISSION_REQUEST);
                            else exportNoteToTxt();
                        }
                    })
                    .show();
            return true;
        } else if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportNoteToTxt();
            } else {
                Toast.makeText(this, "没有存储权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsContentChanged) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.tip))
                    .setMessage("是否保存修改？")
                    .setPositiveButton("保存", (dialog, which) -> saveNote())
                    .setNegativeButton("不保存", (dialog, which) -> finish())
                    .setNeutralButton("取消", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTitleEdit = null;
        mContentEdit = null;
        mContentEditable = null;
    }
}