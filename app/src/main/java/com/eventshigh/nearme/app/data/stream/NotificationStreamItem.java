package com.eventshigh.nearme.app.data.stream;

import android.content.ContentValues;

import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.StreamDbHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationStreamItem extends StreamItem {
  private static final String EVENT_ID = "eventId";
  private static final String EVENT_TITLE = "eventTitle";

  public final String title;

  public NotificationStreamItem(String title) {
    this.title = title;
  }

  public static ContentValues createContentValues(Event event) throws JSONException {
    JSONObject item = new JSONObject();
    item.put(EVENT_ID, event.id);
    item.put(EVENT_TITLE, event.title);

    ContentValues values = StreamItem.createContentValues();
    values.put(StreamDbHelper.COLUMN_TYPE, StreamDbHelper.StreamType.NOTIFICATION.id);
    values.put(StreamDbHelper.COLUMN_BLOB, item.toString());
    return values;
  }

  public static NotificationStreamItem parse(JSONObject json) throws JSONException {
    return new NotificationStreamItem(json.getString(EVENT_TITLE));
  }
}
