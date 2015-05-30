package com.eventshigh.nearme.app.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.data.stream.StreamItem;

import org.json.JSONException;

public class StreamAdapter extends CursorAdapter {
    public StreamAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.stream_item_notification, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView timeView = (TextView) view.findViewById(R.id.time);
        TextView titleView = (TextView) view.findViewById(R.id.title);
        TextView messageView = (TextView) view.findViewById(R.id.message);

        try {
            StreamItem streamItem = StreamDbHelper.parseFromCursor(cursor);

            timeView.setText(DateUtils.getRelativeTimeSpanString(context, streamItem.timestamp, true));
            titleView.setText(streamItem.title);
            messageView.setText(streamItem.message);
        } catch (JSONException e) {
            Crashlytics.logException(e);

            timeView.setText("");
            timeView.setText("Error");
            messageView.setText("Error in loading data. sorry!");
        }
    }
}
