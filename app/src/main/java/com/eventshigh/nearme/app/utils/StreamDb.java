package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.data.stream.NotificationStreamItem;

import org.json.JSONException;

public class StreamDb {
  /**
   * @return the row ID of the newly inserted row, or -1 if an error occurred
   */
  public static long addNotificationStreamItem(Context context, Event event) {
    try {
      StreamDbHelper dbHelper = new StreamDbHelper(context);
      SQLiteDatabase database = dbHelper.getWritableDatabase();
      long retVal = database.insert(StreamDbHelper.TABLE_NAME, null,
          NotificationStreamItem.createContentValues(event));
      dbHelper.close();
      return retVal;
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return -1;
  }
}
