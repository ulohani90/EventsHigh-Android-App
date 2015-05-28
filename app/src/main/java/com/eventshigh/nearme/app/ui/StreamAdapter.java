package com.eventshigh.nearme.app.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.data.stream.NotificationStreamItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class StreamAdapter extends CursorAdapter {
  public static final DateFormat DATE_FORMAT =
      DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());

  public StreamAdapter(Context context, Cursor cursor) {
    super(context, cursor, 0);
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    return LayoutInflater.from(context).inflate(R.layout.stream_item_notification, parent, false);
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    // Find fields to populate in inflated template
    TextView timeView = (TextView) view.findViewById(R.id.time);
    long time = cursor.getLong(cursor.getColumnIndex(StreamDbHelper.COLUMN_TIMESTAMP));
    timeView.setText(DATE_FORMAT.format(new Date(time)));

    TextView description = (TextView) view.findViewById(R.id.description);
    String blob = cursor.getString(cursor.getColumnIndex(StreamDbHelper.COLUMN_BLOB));
    try {
      description.setText(NotificationStreamItem.parse(new JSONObject(blob)).title);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
}
