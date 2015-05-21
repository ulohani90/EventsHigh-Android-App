package com.eventshigh.nearme.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

public class UserActionDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "user_actions.db";
    private static final String USER_ACTIONS_TABLE_NAME = "user_actions";

    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_ACTION = "action";
    private static final String COLUMN_DATA = "data";

    private static final String JSON_KEY_ANDROID_ID = "android_id";
    private static final String JSON_KEY_TIMESTAMP = "timestamp";
    private static final String JSON_KEY_ACTIONS = "actions";
    private static final String JSON_KEY_ACTION = "action";
    private static final String JSON_KEY_DATA = "data";
    private static final String JSON_KEY_EVENT_ID = "event_id";
    private static final String JSON_KEY_INTEREST = "interest";

    private static final String[] ALL_COLUMNS = { COLUMN_TIMESTAMP, COLUMN_ACTION, COLUMN_DATA };

    private static final String CREATE_TABLE =
            "CREATE TABLE " + USER_ACTIONS_TABLE_NAME + " ( "
                    + COLUMN_TIMESTAMP + " INTEGER PRIMARY KEY, "
                    + COLUMN_ACTION + " TEXT, "
                    + COLUMN_DATA + " TEXT "
                    + ");";

    public enum EventAction {
        ADD_FAVORITE,
        REMOVE_FAVORITE,
        BOOK,
        SAVE,
        SHARE,
        VIEW_EVENT,
    }

    public enum FollowingAction {
        FOLLOW,
        UN_FOLLOW,
    }

    private static UserActionDbHelper instance;

    private final Context context;
    private AtomicInteger openCounter = new AtomicInteger();
    private SQLiteDatabase database;

    public static synchronized UserActionDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new UserActionDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private UserActionDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
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

    private synchronized SQLiteDatabase openDatabase() {
        if(openCounter.incrementAndGet() == 1) {
            // Opening new database
            database = getWritableDatabase();
        }
        return database;
    }

    private synchronized void closeDatabase() {
        if(openCounter.decrementAndGet() == 0) {
            // Closing database
            database.close();
        }
    }

    public void recordAction(EventAction action, String eventId) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_KEY_EVENT_ID, eventId);
            recordAction(action.name().toLowerCase(), jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void recordAction(FollowingAction action, String interest) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_KEY_INTEREST, interest);
            recordAction(action.name().toLowerCase(), jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void recordAction(final String action, final String data) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                openDatabase();
                ContentValues values = new ContentValues();
                values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
                values.put(COLUMN_ACTION, action);
                values.put(COLUMN_DATA, data);
                database.insert(USER_ACTIONS_TABLE_NAME, null, values);
                closeDatabase();
            }
        });
        thread.start();
    }

    public JSONObject getActionsSince(long timestamp) throws JSONException {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor =  database.query(USER_ACTIONS_TABLE_NAME, ALL_COLUMNS,
                COLUMN_TIMESTAMP + " > " + timestamp, null, null, null, COLUMN_TIMESTAMP);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_KEY_ANDROID_ID, Utils.getAndroidId(context));
        jsonObject.put(JSON_KEY_TIMESTAMP, timestamp);
        JSONArray actions = new JSONArray();
        jsonObject.put(JSON_KEY_ACTIONS, actions);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            JSONObject action = new JSONObject();
            action.put(JSON_KEY_ACTION, cursor.getString(cursor.getColumnIndex(COLUMN_ACTION)));
            action.put(JSON_KEY_TIMESTAMP, cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP)));
            JSONObject data = new JSONObject(cursor.getString(cursor.getColumnIndex(COLUMN_DATA)));
            action.put(JSON_KEY_DATA, data);
            actions.put(action);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        database.close();
        return jsonObject;
    }
}
