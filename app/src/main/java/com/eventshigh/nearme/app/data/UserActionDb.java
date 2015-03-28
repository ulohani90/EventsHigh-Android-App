package com.eventshigh.nearme.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserActionDb {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "user_actions.db";

    private static final String USER_ACTIONS_TABLE_NAME = "user_actions";

    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_ACTION = "action";
    private static final String COLUMN_EVENT_ID = "event_id";
    private static final String COLUMN_INTEREST = "interest";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + USER_ACTIONS_TABLE_NAME + " ( "
                    + COLUMN_TIMESTAMP + " INTEGER PRIMARY KEY, "
                    + COLUMN_ACTION + " TEXT, "
                    + COLUMN_EVENT_ID + " TEXT, "
                    + COLUMN_INTEREST + " TEXT "
                    + ");";

    public enum EventAction {
        ADD_FAVORITE,
        REMOVE_FAVORITE,
        OPEN_EVENT_DETAIL,
    }

    public enum FollowingAction {
        ADD_FOLLOWING,
        REMOVE_FOLLOWING,
    }

    private SQLiteDatabase database;
    private UserActionsDbHelper dbHelper;

    public UserActionDb(Context context) {
        dbHelper = new UserActionsDbHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        database.close();
    }

    public void recordAction(EventAction action, String eventId) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_ID, eventId);
        recordAction(values, action.name());
    }

    public void recordAction(FollowingAction action, String interest) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_INTEREST, interest);
        recordAction(values, action.name());
    }

    private void recordAction(final ContentValues values, final String action) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
                values.put(COLUMN_ACTION, action);
                database.insert(USER_ACTIONS_TABLE_NAME, null, values);
            }
        });
        thread.start();
    }

    private class UserActionsDbHelper extends SQLiteOpenHelper {
        public UserActionsDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO: Implement proper upgrade. Currently this will delete all user data.
            db.execSQL("DROP TABLE IF EXISTS " + USER_ACTIONS_TABLE_NAME);
            onCreate(db);
        }
    }
}
