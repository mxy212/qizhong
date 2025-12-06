package com.example.android.notepad;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class NotesList extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private NotesAdapter mAdapter;
    private SearchView mSearchView;

    // 布局和主题设置
    private boolean isGridLayout = false;
    private boolean isDarkMode = false;
    private String mSortOrder = NotePad.Notes.DEFAULT_SORT_ORDER;

    private static final String[] PROJECTION = {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_CREATE_DATE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
            NotePad.Notes.COLUMN_NAME_BACK_COLOR
    };

    private final ContentObserver mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            loadNotes(mSearchView != null ? mSearchView.getQuery().toString() : "");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        SharedPreferences layoutPrefs = getSharedPreferences("layout_prefs", MODE_PRIVATE);

        isDarkMode = themePrefs.getBoolean("dark_mode", false);
        isGridLayout = layoutPrefs.getBoolean("is_grid", false);

        setTheme(isDarkMode ? R.style.AppTheme_Dark : R.style.AppTheme_Light);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

        initSearchView();
        bindTopBarButtons();

        mRecyclerView = findViewById(R.id.notes_recycler);
        initRecyclerView();

        mAdapter = new NotesAdapter(isGridLayout);
        mRecyclerView.setAdapter(mAdapter);

        getContentResolver().registerContentObserver(
                NotePad.Notes.CONTENT_URI,
                true,
                mContentObserver
        );

        loadNotes("");
    }

    private void initSearchView() {
        mSearchView = findViewById(R.id.note_search_view);
        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    loadNotes(query);
                    mSearchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    loadNotes(newText);
                    return true;
                }
            });
            mSearchView.setOnCloseListener(() -> {
                mSearchView.setQuery("", false);
                loadNotes("");
                return false;
            });
        }
    }

    private void bindTopBarButtons() {
        View btnAdd = findViewById(R.id.btn_add_note);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> startActivity(new Intent(this, NoteEditor.class)));
        }

        View btnSearch = findViewById(R.id.btn_search);
        if (btnSearch != null && mSearchView != null) {
            btnSearch.setOnClickListener(v -> {
                if (mSearchView.getVisibility() == View.GONE) {
                    mSearchView.setVisibility(View.VISIBLE);
                    mSearchView.requestFocus();
                } else {
                    mSearchView.setVisibility(View.GONE);
                    mSearchView.setQuery("", false);
                    loadNotes("");
                }
            });
        }

        View btnSwitch = findViewById(R.id.btn_switch_layout);
        if (btnSwitch != null) {
            btnSwitch.setOnClickListener(v -> {
                isGridLayout = !isGridLayout;
                getSharedPreferences("layout_prefs", MODE_PRIVATE)
                        .edit().putBoolean("is_grid", isGridLayout).apply();
                initRecyclerView();
                mAdapter = new NotesAdapter(isGridLayout);
                mRecyclerView.setAdapter(mAdapter);
                loadNotes(mSearchView != null ? mSearchView.getQuery().toString() : "");
            });
        }
    }

    private void initRecyclerView() {
        if (isGridLayout) {
            mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        } else {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void loadNotes(String keyword) {
        Cursor cursor;
        ContentResolver resolver = getContentResolver();
        try {
            if (TextUtils.isEmpty(keyword)) {
                cursor = resolver.query(NotePad.Notes.CONTENT_URI, PROJECTION, null, null, mSortOrder);
            } else {
                String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " + NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
                String[] args = {"%" + keyword + "%", "%" + keyword + "%"};
                cursor = resolver.query(NotePad.Notes.CONTENT_URI, PROJECTION, selection, args, mSortOrder);
            }
            if (cursor != null) {
                mAdapter.swapCursor(cursor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class NotesAdapter extends RecyclerView.Adapter<BaseViewHolder> {
        private boolean isGrid;
        private Cursor mCursor;

        public NotesAdapter(boolean isGrid) {
            this.isGrid = isGrid;
        }

        public void swapCursor(Cursor cursor) {
            if (mCursor == cursor) return;
            if (mCursor != null) mCursor.close();
            mCursor = cursor;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (isGrid) {
                View view = inflater.inflate(R.layout.note_item_grid, parent, false);
                return new GridViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.noteslist_item, parent, false);
                return new ListViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
            if (mCursor == null || !mCursor.moveToPosition(position)) return;

            long id = mCursor.getLong(mCursor.getColumnIndexOrThrow(NotePad.Notes._ID));
            String title = mCursor.getString(mCursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_TITLE));
            long createTime = mCursor.getLong(mCursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_CREATE_DATE));
            long modifyTime = mCursor.getLong(mCursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE));
            int colorCode = mCursor.getInt(mCursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_BACK_COLOR));
            int realColor = getColorFromCode(colorCode);

            holder.bind(title, id, createTime, modifyTime, realColor);
        }

        @Override
        public int getItemCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }
    }

    abstract class BaseViewHolder extends RecyclerView.ViewHolder {
        protected long noteId;
        public BaseViewHolder(@NonNull View itemView) { super(itemView); }
        abstract void bind(String title, long id, long createTime, long modifyTime, int realColor);
    }

    class GridViewHolder extends BaseViewHolder {
        View container; TextView title;
        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.note_container);
            title = itemView.findViewById(R.id.note_title);

            itemView.setOnClickListener(v -> {
                if (noteId != -1) openEditor(noteId);
            });
        }

        @Override
        void bind(String titleStr, long id, long createTime, long modifyTime, int realColor) {
            this.noteId = id;
            if(title != null) title.setText(titleStr);
            if (container != null) container.setBackgroundColor(realColor);
        }
    }

    // ==========================================
    // 【修改点】ListViewHolder：修复了 ID 不匹配导致的崩溃
    // ==========================================
    class ListViewHolder extends BaseViewHolder {
        TextView title;
        TextView time;
        ImageView btnEdit;
        ImageView btnDelete;

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            // 绑定 XML 中的控件
            title = itemView.findViewById(android.R.id.text1);

            // 【关键修改】这里必须是 text1_time，因为我们在 noteslist_item.xml 里定义的是这个
            // 同时也是为了和 NoteSearch 中的 SimpleCursorAdapter 保持一致
            time = itemView.findViewById(R.id.text1_time);

            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);

            // 点击整行也可以进入编辑
            itemView.setOnClickListener(v -> {
                if (noteId != -1) openEditor(noteId);
            });

            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    if (noteId != -1) openEditor(noteId);
                });
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    if (noteId != -1) {
                        deleteNote(noteId);
                    }
                });
            }
        }

        @Override
        void bind(String titleStr, long id, long createTime, long modifyTime, int realColor) {
            this.noteId = id;

            // 【安全修改】添加判空检查，防止因为XML配置错误导致程序闪退
            if (title != null) {
                title.setText(titleStr);
                // 简单的文字颜色反转逻辑
                int textColor = isLightColor(realColor) ? Color.BLACK : Color.WHITE;
                title.setTextColor(textColor);
            }

            if (time != null) {
                // 格式化时间
                String timeStr = DateUtils.formatDateTime(NotesList.this, modifyTime,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
                time.setText(timeStr);
                int textColor = isLightColor(realColor) ? Color.BLACK : Color.WHITE;
                time.setTextColor(textColor);
            }

            // 设置背景颜色 (直接设置给整行)
            itemView.setBackgroundColor(realColor);
        }
    }

    private void openEditor(long id) {
        Intent intent = new Intent(NotesList.this, NoteEditor.class);
        intent.setData(ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id));
        startActivity(intent);
    }

    private void deleteNote(long id) {
        getContentResolver().delete(
                ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id),
                null, null
        );
        Toast.makeText(NotesList.this, "已删除", Toast.LENGTH_SHORT).show();
    }

    private int getColorFromCode(int colorCode) {
        switch (colorCode) {
            case NotePad.Notes.YELLOW_COLOR: return 0xFFFFFACD;
            case NotePad.Notes.BLUE_COLOR:   return 0xFFADD8E6;
            case NotePad.Notes.GREEN_COLOR:  return 0xFF90EE90;
            case NotePad.Notes.RED_COLOR:    return 0xFFFFC0CB;
            default: return isDarkMode ? 0xFF333333 : 0xFFFFFFFF;
        }
    }

    private boolean isLightColor(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        return (0.299 * red + 0.587 * green + 0.114 * blue) > 128;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes(mSearchView != null ? mSearchView.getQuery().toString() : "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mContentObserver);
        if (mAdapter != null && mAdapter.mCursor != null) {
            mAdapter.mCursor.close();
        }
    }
}