/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// 声明包名
package net.micode.notes.data;

// 导入搜索管理器相关类
import android.app.SearchManager;
// 导入内容提供者基类
import android.content.ContentProvider;
// 导入用于操作内容 URI 的工具类
import android.content.ContentUris;
// 导入用于存储键值对的内容值类
import android.content.ContentValues;
// 导入意图类
import android.content.Intent;
// 导入 URI 匹配器类
import android.content.UriMatcher;
// 导入数据库游标类
import android.database.Cursor;
// 导入 SQLite 数据库类
import android.database.sqlite.SQLiteDatabase;
// 导入 URI 类
import android.net.Uri;
// 导入文本工具类
import android.text.TextUtils;
// 导入日志工具类
import android.util.Log;

// 导入资源类
import net.micode.notes.R;
// 导入笔记数据列定义类
import net.micode.notes.data.Notes.DataColumns;
// 导入笔记列定义类
import net.micode.notes.data.Notes.NoteColumns;
// 导入笔记数据库帮助类中的表定义
import net.micode.notes.data.NotesDatabaseHelper.TABLE;

/**
 * 自定义的内容提供者类，用于管理笔记数据的访问和操作
 */
public class NotesProvider extends ContentProvider {
    // 定义 URI 匹配器
    private static final UriMatcher mMatcher;
    // 笔记数据库帮助类实例
    private NotesDatabaseHelper mHelper;
    // 日志标签
    private static final String TAG = "NotesProvider";

    // 定义不同 URI 匹配的常量
    private static final int URI_NOTE            = 1;
    private static final int URI_NOTE_ITEM       = 2;
    private static final int URI_DATA            = 3;
    private static final int URI_DATA_ITEM       = 4;
    private static final int URI_SEARCH          = 5;
    private static final int URI_SEARCH_SUGGEST  = 6;

    // 静态代码块，初始化 URI 匹配器
    static {
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        // 添加笔记 URI 匹配规则
        mMatcher.addURI(Notes.AUTHORITY, "note", URI_NOTE);
        mMatcher.addURI(Notes.AUTHORITY, "note/#", URI_NOTE_ITEM);
        // 添加数据 URI 匹配规则
        mMatcher.addURI(Notes.AUTHORITY, "data", URI_DATA);
        mMatcher.addURI(Notes.AUTHORITY, "data/#", URI_DATA_ITEM);
        // 添加搜索 URI 匹配规则
        mMatcher.addURI(Notes.AUTHORITY, "search", URI_SEARCH);
        mMatcher.addURI(Notes.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, URI_SEARCH_SUGGEST);
        mMatcher.addURI(Notes.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", URI_SEARCH_SUGGEST);
    }

    /**
     * 笔记搜索投影字符串，用于定义搜索结果的列
     * x'0A' 表示 SQLite 中的 '\n' 字符，会去除搜索结果中的换行符和空格以显示更多信息
     */
    private static final String NOTES_SEARCH_PROJECTION = NoteColumns.ID + ","
        + NoteColumns.ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA + ","
        + "TRIM(REPLACE(" + NoteColumns.SNIPPET + ", x'0A','')) AS " + SearchManager.SUGGEST_COLUMN_TEXT_1 + ","
        + "TRIM(REPLACE(" + NoteColumns.SNIPPET + ", x'0A','')) AS " + SearchManager.SUGGEST_COLUMN_TEXT_2 + ","
        + R.drawable.search_result + " AS " + SearchManager.SUGGEST_COLUMN_ICON_1 + ","
        + "'" + Intent.ACTION_VIEW + "' AS " + SearchManager.SUGGEST_COLUMN_INTENT_ACTION + ","
        + "'" + Notes.TextNote.CONTENT_TYPE + "' AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA;

    /**
     * 笔记摘要搜索查询语句，用于从数据库中搜索符合条件的笔记
     */
    private static String NOTES_SNIPPET_SEARCH_QUERY = "SELECT " + NOTES_SEARCH_PROJECTION
        + " FROM " + TABLE.NOTE
        + " WHERE " + NoteColumns.SNIPPET + " LIKE ?"
        + " AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER
        + " AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE;

    /**
     * 内容提供者创建时调用，初始化数据库帮助类实例
     * @return 初始化成功返回 true
     */
    @Override
    public boolean onCreate() {
        mHelper = NotesDatabaseHelper.getInstance(getContext());
        return true;
    }

    /**
     * 根据 URI 查询数据库中的数据
     * @param uri 查询的 URI
     * @param projection 查询的列
     * @param selection 查询条件
     * @param selectionArgs 查询条件参数
     * @param sortOrder 排序规则
     * @return 查询结果的游标
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Cursor c = null;
        // 获取可读的数据库实例
        SQLiteDatabase db = mHelper.getReadableDatabase();
        String id = null;
        // 根据 URI 匹配结果执行不同的查询操作
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                c = db.query(TABLE.NOTE, projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            case URI_NOTE_ITEM:
                id = uri.getPathSegments().get(1);
                c = db.query(TABLE.NOTE, projection, NoteColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            case URI_DATA:
                c = db.query(TABLE.DATA, projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            case URI_DATA_ITEM:
                id = uri.getPathSegments().get(1);
                c = db.query(TABLE.DATA, projection, DataColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            case URI_SEARCH:
            case URI_SEARCH_SUGGEST:
                if (sortOrder != null || projection != null) {
                    throw new IllegalArgumentException(
                            "do not specify sortOrder, selection, selectionArgs, or projection" + "with this query");
                }

                String searchString = null;
                if (mMatcher.match(uri) == URI_SEARCH_SUGGEST) {
                    if (uri.getPathSegments().size() > 1) {
                        searchString = uri.getPathSegments().get(1);
                    }
                } else {
                    searchString = uri.getQueryParameter("pattern");
                }

                if (TextUtils.isEmpty(searchString)) {
                    return null;
                }

                try {
                    searchString = String.format("%%%s%%", searchString);
                    c = db.rawQuery(NOTES_SNIPPET_SEARCH_QUERY,
                            new String[] { searchString });
                } catch (IllegalStateException ex) {
                    Log.e(TAG, "got exception: " + ex.toString());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (c != null) {
            // 设置游标监听 URI 变化
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    /**
     * 向数据库中插入数据
     * @param uri 插入数据的 URI
     * @param values 要插入的数据
     * @return 插入数据后的 URI
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // 获取可写的数据库实例
        SQLiteDatabase db = mHelper.getWritableDatabase();
        long dataId = 0, noteId = 0, insertedId = 0;
        // 根据 URI 匹配结果执行不同的插入操作
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                insertedId = noteId = db.insert(TABLE.NOTE, null, values);
                break;
            case URI_DATA:
                if (values.containsKey(DataColumns.NOTE_ID)) {
                    noteId = values.getAsLong(DataColumns.NOTE_ID);
                } else {
                    Log.d(TAG, "Wrong data format without note id:" + values.toString());
                }
                insertedId = dataId = db.insert(TABLE.DATA, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        // 通知笔记 URI 数据变化
        if (noteId > 0) {
            getContext().getContentResolver().notifyChange(
                    ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId), null);
        }

        // 通知数据 URI 数据变化
        if (dataId > 0) {
            getContext().getContentResolver().notifyChange(
                    ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, dataId), null);
        }

        return ContentUris.withAppendedId(uri, insertedId);
    }

    /**
     * 从数据库中删除数据
     * @param uri 删除数据的 URI
     * @param selection 删除条件
     * @param selectionArgs 删除条件参数
     * @return 删除的数据行数
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        String id = null;
        // 获取可写的数据库实例
        SQLiteDatabase db = mHelper.getWritableDatabase();
        boolean deleteData = false;
        // 根据 URI 匹配结果执行不同的删除操作
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                selection = "(" + selection + ") AND " + NoteColumns.ID + ">0 ";
                count = db.delete(TABLE.NOTE, selection, selectionArgs);
                break;
            case URI_NOTE_ITEM:
                id = uri.getPathSegments().get(1);
                /**
                 * ID 小于 0 的是系统文件夹，不允许删除
                 */
                long noteId = Long.valueOf(id);
                if (noteId <= 0) {
                    break;
                }
                count = db.delete(TABLE.NOTE,
                        NoteColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                break;
            case URI_DATA:
                count = db.delete(TABLE.DATA, selection, selectionArgs);
                deleteData = true;
                break;
            case URI_DATA_ITEM:
                id = uri.getPathSegments().get(1);
                count = db.delete(TABLE.DATA,
                        DataColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                deleteData = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (count > 0) {
            if (deleteData) {
                // 通知笔记 URI 数据变化
                getContext().getContentResolver().notifyChange(Notes.CONTENT_NOTE_URI, null);
            }
            // 通知当前 URI 数据变化
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    /**
     * 更新数据库中的数据
     * @param uri 更新数据的 URI
     * @param values 要更新的数据
     * @param selection 更新条件
     * @param selectionArgs 更新条件参数
     * @return 更新的数据行数
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        String id = null;
        // 获取可写的数据库实例
        SQLiteDatabase db = mHelper.getWritableDatabase();
        boolean updateData = false;
        // 根据 URI 匹配结果执行不同的更新操作
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                increaseNoteVersion(-1, selection, selectionArgs);
                count = db.update(TABLE.NOTE, values, selection, selectionArgs);
                break;
            case URI_NOTE_ITEM:
                id = uri.getPathSegments().get(1);
                increaseNoteVersion(Long.valueOf(id), selection, selectionArgs);
                count = db.update(TABLE.NOTE, values, NoteColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs);
                break;
            case URI_DATA:
                count = db.update(TABLE.DATA, values, selection, selectionArgs);
                updateData = true;
                break;
            case URI_DATA_ITEM:
                id = uri.getPathSegments().get(1);
                count = db.update(TABLE.DATA, values, DataColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs);
                updateData = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (count > 0) {
            if (updateData) {
                // 通知笔记 URI 数据变化
                getContext().getContentResolver().notifyChange(Notes.CONTENT_NOTE_URI, null);
            }
            // 通知当前 URI 数据变化
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    /**
     * 解析查询条件，添加额外的 AND 条件
     * @param selection 原始查询条件
     * @return 解析后的查询条件
     */
    private String parseSelection(String selection) {
        return (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
    }

    /**
     * 增加笔记的版本号
     * @param id 笔记的 ID
     * @param selection 更新条件
     * @param selectionArgs 更新条件参数
     */
    private void increaseNoteVersion(long id, String selection, String[] selectionArgs) {
        StringBuilder sql = new StringBuilder(120);
        sql.append("UPDATE ");
        sql.append(TABLE.NOTE);
        sql.append(" SET ");
        sql.append(NoteColumns.VERSION);
        sql.append("=" + NoteColumns.VERSION + "+1 ");

        if (id > 0 || !TextUtils.isEmpty(selection)) {
            sql.append(" WHERE ");
        }
        if (id > 0) {
            sql.append(NoteColumns.ID + "=" + String.valueOf(id));
        }
        if (!TextUtils.isEmpty(selection)) {
            String selectString = id > 0 ? parseSelection(selection) : selection;
            for (String args : selectionArgs) {
                selectString = selectString.replaceFirst("\\?", args);
            }
            sql.append(selectString);
        }

        mHelper.getWritableDatabase().execSQL(sql.toString());
    }

    /**
     * 获取 URI 的 MIME 类型，当前未实现
     * @param uri 查询的 URI
     * @return MIME 类型，当前返回 null
     */
    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

}
