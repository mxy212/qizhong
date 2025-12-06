package com.example.android.notepad;

// 补充必要的导入语句（解决“无法解析符号”错误）
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.graphics.Color;

public class MyCursorAdapter extends SimpleCursorAdapter {

    // 修复1：适配高版本Android的构造函数（添加flags参数，解决构造函数不兼容）
    public MyCursorAdapter(Context context, int layout, Cursor c,
                           String[] from, int[] to, int flags) {
        // 调用父类带flags的构造函数（API 11+ 推荐）
        super(context, layout, c, from, to, flags);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        // 修复2：确保NotePad.Notes中已定义以下常量（否则需补充）
        int colorColumnIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_BACK_COLOR);
        if (colorColumnIndex == -1) {
            // 字段不存在时用默认白色
            view.setBackgroundColor(Color.rgb(255, 255, 255));
            return;
        }

        // 读取颜色字段值（从数据库获取）
        int colorType = cursor.getInt(colorColumnIndex);

        // 设置列表项背景色
        switch (colorType) {
            case NotePad.Notes.WHITE_COLOR:
                view.setBackgroundColor(Color.rgb(255, 255, 255)); // 白色
                break;
            case NotePad.Notes.YELLOW_COLOR:
                view.setBackgroundColor(Color.rgb(247, 216, 133)); // 黄色
                break;
            case NotePad.Notes.BLUE_COLOR:
                view.setBackgroundColor(Color.rgb(165, 202, 237)); // 蓝色
                break;
            case NotePad.Notes.GREEN_COLOR:
                view.setBackgroundColor(Color.rgb(161, 214, 174)); // 绿色
                break;
            case NotePad.Notes.RED_COLOR:
                view.setBackgroundColor(Color.rgb(244, 149, 133)); // 红色
                break;
            default:
                view.setBackgroundColor(Color.rgb(255, 255, 255)); // 默认白色
                break;
        }
    }
}