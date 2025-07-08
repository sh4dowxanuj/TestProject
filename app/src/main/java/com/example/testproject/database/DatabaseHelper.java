package com.example.testproject.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.testproject.models.Bookmark;
import com.example.testproject.models.DownloadItem;
import com.example.testproject.models.HistoryItem;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "browser.db";
    private static final int DATABASE_VERSION = 2;

    // Bookmarks table
    private static final String TABLE_BOOKMARKS = "bookmarks";
    private static final String COLUMN_BOOKMARK_ID = "id";
    private static final String COLUMN_BOOKMARK_TITLE = "title";
    private static final String COLUMN_BOOKMARK_URL = "url";
    private static final String COLUMN_BOOKMARK_TIMESTAMP = "timestamp";

    // History table
    private static final String TABLE_HISTORY = "history";
    private static final String COLUMN_HISTORY_ID = "id";
    private static final String COLUMN_HISTORY_TITLE = "title";
    private static final String COLUMN_HISTORY_URL = "url";
    private static final String COLUMN_HISTORY_TIMESTAMP = "timestamp";

    // Downloads table
    private static final String TABLE_DOWNLOADS = "downloads";
    private static final String COLUMN_DOWNLOAD_ID = "id";
    private static final String COLUMN_DOWNLOAD_TITLE = "title";
    private static final String COLUMN_DOWNLOAD_URL = "url";
    private static final String COLUMN_DOWNLOAD_FILENAME = "filename";
    private static final String COLUMN_DOWNLOAD_FILEPATH = "filepath";
    private static final String COLUMN_DOWNLOAD_FILESIZE = "filesize";
    private static final String COLUMN_DOWNLOAD_DOWNLOADED_SIZE = "downloaded_size";
    private static final String COLUMN_DOWNLOAD_STATUS = "status";
    private static final String COLUMN_DOWNLOAD_TIMESTAMP = "timestamp";
    private static final String COLUMN_DOWNLOAD_MIMETYPE = "mimetype";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_BOOKMARKS_TABLE = "CREATE TABLE " + TABLE_BOOKMARKS + "("
                + COLUMN_BOOKMARK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_BOOKMARK_TITLE + " TEXT,"
                + COLUMN_BOOKMARK_URL + " TEXT UNIQUE,"
                + COLUMN_BOOKMARK_TIMESTAMP + " INTEGER" + ")";

        String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HISTORY + "("
                + COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_HISTORY_TITLE + " TEXT,"
                + COLUMN_HISTORY_URL + " TEXT,"
                + COLUMN_HISTORY_TIMESTAMP + " INTEGER" + ")";

        String CREATE_DOWNLOADS_TABLE = "CREATE TABLE " + TABLE_DOWNLOADS + "("
                + COLUMN_DOWNLOAD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_DOWNLOAD_TITLE + " TEXT,"
                + COLUMN_DOWNLOAD_URL + " TEXT,"
                + COLUMN_DOWNLOAD_FILENAME + " TEXT,"
                + COLUMN_DOWNLOAD_FILEPATH + " TEXT,"
                + COLUMN_DOWNLOAD_FILESIZE + " INTEGER,"
                + COLUMN_DOWNLOAD_DOWNLOADED_SIZE + " INTEGER,"
                + COLUMN_DOWNLOAD_STATUS + " INTEGER,"
                + COLUMN_DOWNLOAD_TIMESTAMP + " INTEGER,"
                + COLUMN_DOWNLOAD_MIMETYPE + " TEXT" + ")";

        db.execSQL(CREATE_BOOKMARKS_TABLE);
        db.execSQL(CREATE_HISTORY_TABLE);
        db.execSQL(CREATE_DOWNLOADS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOWNLOADS);
        onCreate(db);
    }

    // Bookmark methods
    public long addBookmark(Bookmark bookmark) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BOOKMARK_TITLE, bookmark.getTitle());
        values.put(COLUMN_BOOKMARK_URL, bookmark.getUrl());
        values.put(COLUMN_BOOKMARK_TIMESTAMP, bookmark.getTimestamp());

        long id = db.insertWithOnConflict(TABLE_BOOKMARKS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return id;
    }

    public List<Bookmark> getAllBookmarks() {
        List<Bookmark> bookmarks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_BOOKMARKS + " ORDER BY " + COLUMN_BOOKMARK_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Bookmark bookmark = new Bookmark();
                bookmark.setId(cursor.getLong(0));
                bookmark.setTitle(cursor.getString(1));
                bookmark.setUrl(cursor.getString(2));
                bookmark.setTimestamp(cursor.getLong(3));
                bookmarks.add(bookmark);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return bookmarks;
    }

    public void deleteBookmark(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_BOOKMARKS, COLUMN_BOOKMARK_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public boolean isBookmarked(String url) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKMARKS, null, COLUMN_BOOKMARK_URL + " = ?", 
                new String[]{url}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // History methods
    public long addHistoryItem(HistoryItem historyItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HISTORY_TITLE, historyItem.getTitle());
        values.put(COLUMN_HISTORY_URL, historyItem.getUrl());
        values.put(COLUMN_HISTORY_TIMESTAMP, historyItem.getTimestamp());

        long id = db.insert(TABLE_HISTORY, null, values);
        db.close();
        return id;
    }

    public List<HistoryItem> getAllHistory() {
        List<HistoryItem> historyItems = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + COLUMN_HISTORY_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                HistoryItem historyItem = new HistoryItem();
                historyItem.setId(cursor.getLong(0));
                historyItem.setTitle(cursor.getString(1));
                historyItem.setUrl(cursor.getString(2));
                historyItem.setTimestamp(cursor.getLong(3));
                historyItems.add(historyItem);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return historyItems;
    }

    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
        db.close();
    }

    public void deleteHistoryItem(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, COLUMN_HISTORY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Search methods for suggestions
    public List<HistoryItem> searchHistory(String query, int limit) {
        List<HistoryItem> historyItems = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HISTORY + 
                " WHERE " + COLUMN_HISTORY_TITLE + " LIKE ? OR " + COLUMN_HISTORY_URL + " LIKE ?" +
                " ORDER BY " + COLUMN_HISTORY_TIMESTAMP + " DESC LIMIT ?";

        SQLiteDatabase db = this.getReadableDatabase();
        String searchPattern = "%" + query + "%";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{searchPattern, searchPattern, String.valueOf(limit)});

        if (cursor.moveToFirst()) {
            do {
                HistoryItem historyItem = new HistoryItem();
                historyItem.setId(cursor.getLong(0));
                historyItem.setTitle(cursor.getString(1));
                historyItem.setUrl(cursor.getString(2));
                historyItem.setTimestamp(cursor.getLong(3));
                historyItems.add(historyItem);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return historyItems;
    }

    public List<Bookmark> searchBookmarks(String query, int limit) {
        List<Bookmark> bookmarks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_BOOKMARKS + 
                " WHERE " + COLUMN_BOOKMARK_TITLE + " LIKE ? OR " + COLUMN_BOOKMARK_URL + " LIKE ?" +
                " ORDER BY " + COLUMN_BOOKMARK_TIMESTAMP + " DESC LIMIT ?";

        SQLiteDatabase db = this.getReadableDatabase();
        String searchPattern = "%" + query + "%";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{searchPattern, searchPattern, String.valueOf(limit)});

        if (cursor.moveToFirst()) {
            do {
                Bookmark bookmark = new Bookmark();
                bookmark.setId(cursor.getLong(0));
                bookmark.setTitle(cursor.getString(1));
                bookmark.setUrl(cursor.getString(2));
                bookmark.setTimestamp(cursor.getLong(3));
                bookmarks.add(bookmark);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return bookmarks;
    }

    // Download methods
    public long addDownloadItem(DownloadItem downloadItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DOWNLOAD_TITLE, downloadItem.getTitle());
        values.put(COLUMN_DOWNLOAD_URL, downloadItem.getUrl());
        values.put(COLUMN_DOWNLOAD_FILENAME, downloadItem.getFileName());
        values.put(COLUMN_DOWNLOAD_FILEPATH, downloadItem.getFilePath());
        values.put(COLUMN_DOWNLOAD_FILESIZE, downloadItem.getFileSize());
        values.put(COLUMN_DOWNLOAD_DOWNLOADED_SIZE, downloadItem.getDownloadedSize());
        values.put(COLUMN_DOWNLOAD_STATUS, downloadItem.getStatus());
        values.put(COLUMN_DOWNLOAD_TIMESTAMP, downloadItem.getTimestamp());
        values.put(COLUMN_DOWNLOAD_MIMETYPE, downloadItem.getMimeType());

        long id = db.insert(TABLE_DOWNLOADS, null, values);
        db.close();
        return id;
    }

    public List<DownloadItem> getAllDownloads() {
        List<DownloadItem> downloadItems = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_DOWNLOADS + " ORDER BY " + COLUMN_DOWNLOAD_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                DownloadItem downloadItem = new DownloadItem();
                downloadItem.setId(cursor.getLong(0));
                downloadItem.setTitle(cursor.getString(1));
                downloadItem.setUrl(cursor.getString(2));
                downloadItem.setFileName(cursor.getString(3));
                downloadItem.setFilePath(cursor.getString(4));
                downloadItem.setFileSize(cursor.getLong(5));
                downloadItem.setDownloadedSize(cursor.getLong(6));
                downloadItem.setStatus(cursor.getInt(7));
                downloadItem.setTimestamp(cursor.getLong(8));
                downloadItem.setMimeType(cursor.getString(9));
                downloadItems.add(downloadItem);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return downloadItems;
    }

    public void deleteDownloadItem(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DOWNLOADS, COLUMN_DOWNLOAD_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void updateDownload(DownloadItem downloadItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DOWNLOAD_DOWNLOADED_SIZE, downloadItem.getDownloadedSize());
        values.put(COLUMN_DOWNLOAD_STATUS, downloadItem.getStatus());
        values.put(COLUMN_DOWNLOAD_FILESIZE, downloadItem.getFileSize());
        
        db.update(TABLE_DOWNLOADS, values, COLUMN_DOWNLOAD_ID + " = ?", 
                 new String[]{String.valueOf(downloadItem.getId())});
        db.close();
    }

    public void clearDownloads() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DOWNLOADS, null, null);
        db.close();
    }
}
