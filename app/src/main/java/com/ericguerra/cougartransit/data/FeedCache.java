package com.ericguerra.cougartransit.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashSet;
import java.util.Set;

/**
 * SQLite storage for the last good feed (so the app works offline / on flaky campus Wi-Fi)
 * and for the user's favorite stops.
 */
public final class FeedCache extends SQLiteOpenHelper {
    private static final String DB_NAME = "transit.db";
    private static final int VERSION = 2;

    public FeedCache(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE feed (id INTEGER PRIMARY KEY CHECK (id = 1), json TEXT NOT NULL, fetched_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE favorites (stop_id TEXT PRIMARY KEY, added_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS favorites (stop_id TEXT PRIMARY KEY, added_at INTEGER NOT NULL)");
        }
    }

    public void saveFeed(String json, long fetchedAt) {
        ContentValues v = new ContentValues();
        v.put("id", 1);
        v.put("json", json);
        v.put("fetched_at", fetchedAt);
        getWritableDatabase().insertWithOnConflict("feed", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Returns {json, fetchedAt} or null. */
    public Object[] loadFeed() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT json, fetched_at FROM feed WHERE id = 1", null)) {
            return c.moveToFirst() ? new Object[]{c.getString(0), c.getLong(1)} : null;
        }
    }

    public Set<String> favorites() {
        Set<String> out = new HashSet<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT stop_id FROM favorites", null)) {
            while (c.moveToNext()) out.add(c.getString(0));
        }
        return out;
    }

    public boolean toggleFavorite(String stopId) {
        SQLiteDatabase db = getWritableDatabase();
        int removed = db.delete("favorites", "stop_id = ?", new String[]{stopId});
        if (removed > 0) return false;
        ContentValues v = new ContentValues();
        v.put("stop_id", stopId);
        v.put("added_at", System.currentTimeMillis());
        db.insert("favorites", null, v);
        return true;
    }
}
