package com.eventshigh.nearme.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.stream.CustomUrlNotificationStream;
import com.eventshigh.nearme.app.data.stream.EventNotificationStreamItem;
import com.eventshigh.nearme.app.data.stream.QueryNotificationStreamItem;
import com.eventshigh.nearme.app.data.stream.StreamItem;
import com.eventshigh.nearme.app.data.stream.TicketNotificationStreamItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StreamDbHelper extends SQLiteOpenHelper {
    private static final String TABLE_NAME = "stream";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_BLOB = "blob";

    private static final String DATABASE_NAME = "stream.db";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE = "create table "
        + TABLE_NAME + "("
        + COLUMN_ID + " integer primary key autoincrement, "
        + COLUMN_TIMESTAMP + " integer not null, "
        + COLUMN_TYPE + " integer not null, "
        + COLUMN_BLOB + " blob);";

    public enum StreamType {
        EVENT_NOTIFICATION(1),
        QUERY_NOTIFICATION(2),
        TICKET_NOTIFICATION(3),
        CUSTOM_URL_NOTIFICATION(4);

        public final int id;

        StreamType(int id) {
            this.id = id;
        }
    }

    public StreamDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Nothing to upgrade yet
    }

    /**
     * @return the row ID of the newly inserted row.
     */
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static long addStreamToDB(Context context, StreamItem streamItem) throws JSONException {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TIMESTAMP, streamItem.timestamp);
        values.put(COLUMN_TYPE, streamItem.getStreamType().id);
        values.put(COLUMN_BLOB, streamItem.toJSON().toString());

        StreamDbHelper dbHelper = new StreamDbHelper(context);
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        try {
            return database.insert(StreamDbHelper.TABLE_NAME, null, values);
        } finally {
            database.close();
            dbHelper.close();
        }
    }

    public static StreamItem parseFromCursor(Cursor cursor) throws JSONException {
        long time = cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP));
        int type = cursor.getInt(cursor.getColumnIndex(COLUMN_TYPE));
        String blob = cursor.getString(cursor.getColumnIndex(COLUMN_BLOB));
        JSONObject jsonObject = new JSONObject(blob);

        if (type == StreamType.EVENT_NOTIFICATION.id) {
            return new EventNotificationStreamItem(time, jsonObject);
        }

        if (type == StreamType.QUERY_NOTIFICATION.id) {
            return new QueryNotificationStreamItem(time, jsonObject);
        }

        if (type == StreamType.TICKET_NOTIFICATION.id) {
            return new TicketNotificationStreamItem(time, jsonObject);
        }

        if(type == StreamType.CUSTOM_URL_NOTIFICATION.id){
            return new CustomUrlNotificationStream(time,jsonObject);
        }

        throw new IllegalArgumentException("invalid type: " + type);
    }

    public static Cursor getCursorToStreamItems(Context context) {
        StreamDbHelper dbHelper = new StreamDbHelper(context);
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        return database.rawQuery("select * from " + StreamDbHelper.TABLE_NAME + " order by "
                + StreamDbHelper.COLUMN_TIMESTAMP + " desc limit 20;", null);
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static List<StreamItem> getStreamItems(Context context) {
        List<StreamItem> streamItems = new ArrayList<>();
        Cursor cursor = getCursorToStreamItems(context);

        try {
            while (cursor.moveToNext()) {
                try {
                    StreamItem item = parseFromCursor(cursor);
                    if (streamItems.isEmpty() || !streamItems.get(streamItems.size() - 1).equals(item)) {
                        streamItems.add(item);
                    }
                } catch (JSONException e) {
                    Crashlytics.getInstance().core.logException(e);
                }
            }
        } finally {
            cursor.close();
        }

        return streamItems;
    }
}
