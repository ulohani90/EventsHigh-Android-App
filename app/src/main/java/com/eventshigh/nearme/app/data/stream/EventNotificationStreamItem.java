package com.eventshigh.nearme.app.data.stream;

import android.content.Context;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.data.StreamDbHelper.StreamType;

import org.json.JSONException;
import org.json.JSONObject;

public class EventNotificationStreamItem extends StreamItem {
    public final String eventId;

    // Record event notification as stream item.
    public static void record(Context context, String title, String message,
              @Nullable String imgUrl, String id) {
        EventNotificationStreamItem streamItem = new EventNotificationStreamItem(
                System.currentTimeMillis(), title, message, imgUrl, id);
        try {
            StreamDbHelper.addStreamToDB(context, streamItem);
        } catch (JSONException e) {
            Crashlytics.logException(e);
        }
    }

    public EventNotificationStreamItem(long timestamp, String title, String message,
              @Nullable String imgUrl, String eventId) {
        super(timestamp, title, message, imgUrl);

        this.eventId = eventId;
    }

    public EventNotificationStreamItem(long timestamp, JSONObject json) throws JSONException {
        super(timestamp, json);

        this.eventId = json.getString("event_id");
    }

    public StreamType getStreamType() {
        return StreamType.EVENT_NOTIFICATION;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = super.toJSON();
        jsonObject.put("event_id", eventId);
        return jsonObject;
    }

}
