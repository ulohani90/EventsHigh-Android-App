package com.eventshigh.nearme.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Created by umesh on 13/05/16.
 * <p/>
 * Helper class to manage the SQLite db storing the
 * {@link com.eventshigh.nearme.app.data.MovieMarkerManager.MovieMark} which
 * includes the favourite and dismissed movies.
 */
public class MovieMarkDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "movie_marks.db";
    private static final String MOVIE_PREFS_TABLE_NAME = "movie_marks";
    private static final String MOVIE_PREFS_COLUMN_MOVIE_ID = "movie_id";
    private static final String MOVIE_PREFS_COLUMN_MOVIE_MARK = "movie_mark";
    private static final String MOVIE_PREFS_COLUMN_UPDATED_AT = "updated_at";

    private static final String MOVIE_PREFS_TABLE_CREATE =
            "CREATE TABLE " + MOVIE_PREFS_TABLE_NAME + " ( " + MOVIE_PREFS_COLUMN_MOVIE_ID +
                    " TEXT PRIMARY KEY, " + MOVIE_PREFS_COLUMN_MOVIE_MARK + " INTEGER, " +
                    MOVIE_PREFS_COLUMN_UPDATED_AT + " INTEGER );";

    public MovieMarkDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(MOVIE_PREFS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // TODO: Implement proper upgrade. Currently this will delete all user data.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MOVIE_PREFS_TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    static Thread addEntry(final SQLiteDatabase database, final String movieId, final MovieMarkerManager.MovieMark movieMark) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(MOVIE_PREFS_COLUMN_MOVIE_ID, movieId);
                values.put(MOVIE_PREFS_COLUMN_MOVIE_MARK, movieMark.value);
                values.put(MOVIE_PREFS_COLUMN_UPDATED_AT, (int) (System.currentTimeMillis() / 1000));
                database.replace(MOVIE_PREFS_TABLE_NAME, null, values);
            }
        });
        thread.start();
        return thread;
    }

    static Thread removeEntry(final SQLiteDatabase database, final String movieId) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                database.delete(MOVIE_PREFS_TABLE_NAME,
                        MOVIE_PREFS_COLUMN_MOVIE_ID + " = '" + movieId + "'", null);
            }
        });
        thread.start();
        return thread;
    }

    static Iterable<Pair<String, MovieMarkerManager.MovieMark>> fetchAllEntries(final SQLiteDatabase database) {
        final long aMonthAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        return new Iterable<Pair<String, MovieMarkerManager.MovieMark>>() {
            @Override
            public Iterator<Pair<String, MovieMarkerManager.MovieMark>> iterator() {
                final Cursor cursor = database.query(MOVIE_PREFS_TABLE_NAME, null,
                        MOVIE_PREFS_COLUMN_UPDATED_AT + " > " + (aMonthAgo / 1000), null, null, null, null);
                cursor.moveToFirst();
                return new Iterator<Pair<String, MovieMarkerManager.MovieMark>>() {
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
                    public Pair<String, MovieMarkerManager.MovieMark> next() {
                        Pair<String, MovieMarkerManager.MovieMark> ret = Pair.create(
                                cursor.getString(0), MovieMarkerManager.MovieMark.getPrefFromValue(cursor.getInt(1)));
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
