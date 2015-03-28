package com.eventshigh.nearme.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserActionsDbHelper extends SQLiteOpenHelper {
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
