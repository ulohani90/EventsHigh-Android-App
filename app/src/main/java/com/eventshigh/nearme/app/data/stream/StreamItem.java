package com.eventshigh.nearme.app.data.stream;

import android.content.ContentValues;

import com.eventshigh.nearme.app.data.StreamDbHelper;

public class StreamItem {
  private long timestamp;

  public static ContentValues createContentValues() {
    ContentValues values = new ContentValues();
    values.put(StreamDbHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());
    return values;
  }
}
