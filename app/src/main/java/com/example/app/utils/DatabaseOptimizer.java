package com.example.app.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Database optimization utility for better performance
 */
public class DatabaseOptimizer {
    private static final String TAG = "DatabaseOptimizer";
    
    private static DatabaseOptimizer instance;
    private final Context context;
    private final HandlerThread dbThread;
    private final Handler dbHandler;
    private final Handler mainHandler;
    private final ConcurrentLinkedQueue<Runnable> pendingOperations;
    private final AtomicBoolean isProcessing;
    
    private DatabaseOptimizer(Context context) {
        this.context = context.getApplicationContext();
        this.dbThread = new HandlerThread("DatabaseOptimizer");
        this.dbThread.start();
        this.dbHandler = new Handler(dbThread.getLooper());
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.pendingOperations = new ConcurrentLinkedQueue<>();
        this.isProcessing = new AtomicBoolean(false);
    }
    
    public static synchronized DatabaseOptimizer getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseOptimizer(context);
        }
        return instance;
    }
    
    /**
     * Execute database operation on background thread
     */
    public void executeAsync(Runnable operation) {
        dbHandler.post(operation);
    }
    
    /**
     * Execute database operation on background thread with callback
     */
    public void executeAsync(Runnable operation, Runnable callback) {
        dbHandler.post(() -> {
            try {
                operation.run();
                if (callback != null) {
                    mainHandler.post(callback);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Batch database operations for better performance
     */
    public void batchExecute(Runnable... operations) {
        dbHandler.post(() -> {
            for (Runnable operation : operations) {
                try {
                    operation.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * Optimize database performance
     */
    public void optimizeDatabase(SQLiteDatabase db) {
        if (db == null) return;
        
        dbHandler.post(() -> {
            try {
                // Enable Write-Ahead Logging for better concurrency
                db.enableWriteAheadLogging();
                
                // Set journal mode to WAL for better performance
                db.execSQL("PRAGMA journal_mode=WAL");
                
                // Optimize synchronous setting
                db.execSQL("PRAGMA synchronous=NORMAL");
                
                // Set cache size (negative value means KB)
                db.execSQL("PRAGMA cache_size=-10000"); // 10MB cache
                
                // Set page size for better performance
                db.execSQL("PRAGMA page_size=4096");
                
                // Enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys=ON");
                
                // Set temp store to memory
                db.execSQL("PRAGMA temp_store=MEMORY");
                
                // Set mmap size for better read performance
                db.execSQL("PRAGMA mmap_size=268435456"); // 256MB
                
                // Analyze database for query optimization
                db.execSQL("ANALYZE");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Clean up database
     */
    public void cleanup(SQLiteDatabase db) {
        if (db == null) return;
        
        dbHandler.post(() -> {
            try {
                // Vacuum database to reclaim space
                db.execSQL("VACUUM");
                
                // Update statistics
                db.execSQL("ANALYZE");
                
                // Checkpoint WAL file
                db.execSQL("PRAGMA wal_checkpoint(FULL)");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Create database indices for better query performance
     */
    public void createIndices(SQLiteDatabase db) {
        if (db == null) return;
        
        dbHandler.post(() -> {
            try {
                // Create index on history table
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_url ON history(url)");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_title ON history(title)");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_timestamp ON history(timestamp)");
                
                // Create index on bookmarks table
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_url ON bookmarks(url)");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_title ON bookmarks(title)");
                
                // Create index on downloads table
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_downloads_filename ON downloads(filename)");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_downloads_timestamp ON downloads(timestamp)");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_downloads_status ON downloads(status)");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Get database statistics
     */
    public void getDatabaseStats(SQLiteDatabase db, DatabaseStatsCallback callback) {
        if (db == null || callback == null) return;
        
        dbHandler.post(() -> {
            try {
                DatabaseStats stats = new DatabaseStats();
                
                // Get page count
                android.database.Cursor cursor = db.rawQuery("PRAGMA page_count", null);
                if (cursor.moveToFirst()) {
                    stats.pageCount = cursor.getInt(0);
                }
                cursor.close();
                
                // Get page size
                cursor = db.rawQuery("PRAGMA page_size", null);
                if (cursor.moveToFirst()) {
                    stats.pageSize = cursor.getInt(0);
                }
                cursor.close();
                
                // Get cache size
                cursor = db.rawQuery("PRAGMA cache_size", null);
                if (cursor.moveToFirst()) {
                    stats.cacheSize = cursor.getInt(0);
                }
                cursor.close();
                
                // Calculate database size
                stats.databaseSize = (long) stats.pageCount * stats.pageSize;
                
                // Get table counts
                cursor = db.rawQuery("SELECT COUNT(*) FROM history", null);
                if (cursor.moveToFirst()) {
                    stats.historyCount = cursor.getInt(0);
                }
                cursor.close();
                
                cursor = db.rawQuery("SELECT COUNT(*) FROM bookmarks", null);
                if (cursor.moveToFirst()) {
                    stats.bookmarkCount = cursor.getInt(0);
                }
                cursor.close();
                
                cursor = db.rawQuery("SELECT COUNT(*) FROM downloads", null);
                if (cursor.moveToFirst()) {
                    stats.downloadCount = cursor.getInt(0);
                }
                cursor.close();
                
                mainHandler.post(() -> callback.onStatsReady(stats));
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onStatsReady(null));
            }
        });
    }
    
    /**
     * Shutdown database optimizer
     */
    public void shutdown() {
        if (dbThread != null) {
            dbThread.quitSafely();
        }
    }
    
    /**
     * Database statistics callback
     */
    public interface DatabaseStatsCallback {
        void onStatsReady(DatabaseStats stats);
    }
    
    /**
     * Database statistics class
     */
    public static class DatabaseStats {
        public int pageCount;
        public int pageSize;
        public int cacheSize;
        public long databaseSize;
        public int historyCount;
        public int bookmarkCount;
        public int downloadCount;
        
        public String getFormattedSize() {
            if (databaseSize < 1024) {
                return databaseSize + " B";
            } else if (databaseSize < 1024 * 1024) {
                return (databaseSize / 1024) + " KB";
            } else {
                return (databaseSize / (1024 * 1024)) + " MB";
            }
        }
        
        public String getSummary() {
            return "Database Size: " + getFormattedSize() + "\n" +
                   "History Items: " + historyCount + "\n" +
                   "Bookmarks: " + bookmarkCount + "\n" +
                   "Downloads: " + downloadCount;
        }
    }
}
