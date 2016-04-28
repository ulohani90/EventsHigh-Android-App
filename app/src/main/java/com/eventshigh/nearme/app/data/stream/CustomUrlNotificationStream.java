package com.eventshigh.nearme.app.data.stream;

import android.content.Context;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by umesh on 27/04/16.
 */
public class CustomUrlNotificationStream extends StreamItem {

    private static final String CUSTOM_URL_KEY = "target_url";

    public final String customUrl;


    // Record event notification as stream item.
    public static void record(Context context, String title, String message,
                              @Nullable String imgUrl, @Nullable String mobileNo, String id) {
        CustomUrlNotificationStream streamItem = new CustomUrlNotificationStream(
                System.currentTimeMillis(), title, message, imgUrl, mobileNo, id);
        try {
            StreamDbHelper.addStreamToDB(context, streamItem);
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
        }

    }

    public CustomUrlNotificationStream(long timestamp, String title, String message,
                                       @Nullable String imgUrl, @Nullable String mobileNo, String customUrl) {
        super(timestamp, title, message, imgUrl, mobileNo);

        this.customUrl = customUrl;

    }

    public CustomUrlNotificationStream(long timestamp, JSONObject json) throws JSONException {
        super(timestamp, json);
        this.customUrl = json.getString(CUSTOM_URL_KEY);

    }

    public StreamDbHelper.StreamType getStreamType() {
        return StreamDbHelper.StreamType.CUSTOM_URL_NOTIFICATION;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = super.toJSON();
        jsonObject.put(CUSTOM_URL_KEY, customUrl);
        return jsonObject;
    }

    public void launch(BaseContextActivity activity) {
        activity.reportActionToAnalytics("streamItemClick", customUrl);
        activity.showCustomUrlActivity(customUrl,title);
    }
}
