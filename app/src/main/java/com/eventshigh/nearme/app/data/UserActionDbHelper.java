package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

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

    public void recordShareAction(String eventId, @Nullable String appName, @Nullable String postId) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_KEY_EVENT_ID, eventId);
            if (appName != null) {
                jsonObject.put("app_name", appName);
            }
            if (postId != null) {
                jsonObject.put("post_id", postId);
            }
            recordAction(EventAction.SHARE.name().toLowerCase(), jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
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
        recordAction(action, data, System.currentTimeMillis());
    }

    private void recordAction(final String action, final String data, final long timestamp) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle bundle = new Bundle();
                bundle.putString(JSON_KEY_ANDROID_ID, Utils.getAndroidId(context));
                bundle.putString(JSON_KEY_TIMESTAMP, Long.toString(timestamp));
                bundle.putString(JSON_KEY_ACTION, action);
                bundle.putString(JSON_KEY_DATA, data);
                GcmRegistration.sendUpstream(context, UUID.randomUUID().toString(), bundle);
            }
        });
        thread.start();
    }

    public void reportActionSince(long timestamp) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor =  database.query(USER_ACTIONS_TABLE_NAME, ALL_COLUMNS,
                COLUMN_TIMESTAMP + " > " + timestamp, null, null, null, COLUMN_TIMESTAMP);
        try {
            while (!cursor.isAfterLast()) {
                recordAction(cursor.getString(cursor.getColumnIndex(COLUMN_ACTION)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_DATA)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP)));
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
            database.close();
        }
    }
}
