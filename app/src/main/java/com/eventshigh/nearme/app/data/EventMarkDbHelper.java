package com.eventshigh.nearme.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;

import java.util.Iterator;

/**
 * Helper class to manage the SQLite db storing the
 * {@link com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark} which
 * includes the favourite and dismissed events.
 */
public class EventMarkDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "event_marks.db";
    private static final String EVENT_PREFS_TABLE_NAME = "event_marks";
    private static final String EVENT_PREFS_COLUMN_EVENT_ID = "event_id";
    private static final String EVENT_PREFS_COLUMN_EVENT_MARK = "event_mark";
    private static final String EVENT_PREFS_COLUMN_UPDATED_AT = "updated_at";

    private static final String EVENT_PREFS_TABLE_CREATE =
        "CREATE TABLE " + EVENT_PREFS_TABLE_NAME + " ( " + EVENT_PREFS_COLUMN_EVENT_ID +
        " TEXT PRIMARY KEY, " + EVENT_PREFS_COLUMN_EVENT_MARK + " INTEGER, " +
        EVENT_PREFS_COLUMN_UPDATED_AT + " INTEGER );";

    public EventMarkDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(EVENT_PREFS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // TODO: Implement proper upgrade. Currently this will delete all user data.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + EVENT_PREFS_TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    static void addEntry(final SQLiteDatabase database, final String eventId, final EventMark eventMark) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(EVENT_PREFS_COLUMN_EVENT_ID, eventId);
                values.put(EVENT_PREFS_COLUMN_EVENT_MARK, eventMark.value);
                values.put(EVENT_PREFS_COLUMN_UPDATED_AT, (int) (System.currentTimeMillis() / 1000));
                database.replace(EVENT_PREFS_TABLE_NAME, null, values);
            }
        }).start();
    }

    static void removeEntry(final SQLiteDatabase database, final String eventId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                database.delete(EVENT_PREFS_TABLE_NAME,
                        EVENT_PREFS_COLUMN_EVENT_ID + " = '" + eventId + "'", null);
            }
        }).start();
    }

    static Iterable<Pair<String, EventMark>> fetchAllEntries(final SQLiteDatabase database) {
        return new Iterable<Pair<String, EventMark>>() {
            @Override
            public Iterator<Pair<String, EventMark>> iterator() {
                final Cursor cursor = database.query(EVENT_PREFS_TABLE_NAME, null, null, null, null, null, null);
                cursor.moveToFirst();
                return new Iterator<Pair<String, EventMark>>() {
                    @Override
                    public boolean hasNext() {
                        if (cursor.isClosed()) {
                            return false;
                        }

                        if (cursor.isAfterLast()) {
                            cursor.close();
                            return false;
                        }

                        return true;
                    }

                    @Override
                    public Pair<String, EventMark> next() {
                        Pair<String, EventMark> ret = Pair.create(
                                cursor.getString(0), EventMark.getPrefFromValue(cursor.getInt(1)));
                        cursor.moveToNext();
                        return ret;
                    }

                    @Override
                    public void remove() {
                        // Not supported.
                        throw new UnsupportedOperationException("remove() is not supported");
                    }
                };
            }
        };
    }
}
