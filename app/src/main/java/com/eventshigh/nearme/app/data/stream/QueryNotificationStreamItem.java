package com.eventshigh.nearme.app.data.stream;

import android.content.Context;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.data.StreamDbHelper.StreamType;

import org.json.JSONException;
import org.json.JSONObject;

public class QueryNotificationStreamItem extends StreamItem {
    public final String query;

    // Record event notification as stream item.
    public static void record(Context context, String title, String message,
                              @Nullable String imgUrl, String query) {
        QueryNotificationStreamItem streamItem = new QueryNotificationStreamItem(
                System.currentTimeMillis(), title, message, imgUrl, query);
        try {
            StreamDbHelper.addStreamToDB(context, streamItem);
        } catch (JSONException e) {
            Crashlytics.logException(e);
        }
    }

    public QueryNotificationStreamItem(long timestamp, String title, String message,
                                       @Nullable String imgUrl, String query) {
        super(timestamp, title, message, imgUrl);

        this.query = query;
    }

    public QueryNotificationStreamItem(long timestamp, JSONObject json) throws JSONException {
        super(timestamp, json);

        this.query = json.getString("query");
    }

    public StreamType getStreamType() {
        return StreamType.QUERY_NOTIFICATION;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = super.toJSON();
        jsonObject.put("query", query);
        return jsonObject;
    }

    public void launch(BaseContextActivity activity) {
        activity.showSearchView(query);
    }
}
