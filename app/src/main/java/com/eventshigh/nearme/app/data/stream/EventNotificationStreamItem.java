package com.eventshigh.nearme.app.data.stream;

import android.content.Context;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.data.StreamDbHelper.StreamType;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

public class EventNotificationStreamItem extends StreamItem {
    private static final String EVENT_ID_KEY = "event_id";
    private static final String CITY_KEY = "city";

    public final String eventId;
    public final City city;

    // Record event notification as stream item.
    public static void record(Context context, String title, String message,
              @Nullable String imgUrl, @Nullable String mobileNo, String id, City city) {
        EventNotificationStreamItem streamItem = new EventNotificationStreamItem(
                System.currentTimeMillis(), title, message, imgUrl, mobileNo, id, city);
        try {
            StreamDbHelper.addStreamToDB(context, streamItem);
        } catch (JSONException e) {
            // Ignore.
        }
    }

    public EventNotificationStreamItem(long timestamp, String title, String message,
              @Nullable String imgUrl, @Nullable String mobileNo, String eventId, City city) {
        super(timestamp, title, message, imgUrl, mobileNo);

        this.eventId = eventId;
        this.city = city;
    }

    public EventNotificationStreamItem(long timestamp, JSONObject json) throws JSONException {
        super(timestamp, json);

        this.eventId = json.getString(EVENT_ID_KEY);
        this.city = City.getCity(json.getString(CITY_KEY));
    }

    public StreamType getStreamType() {
        return StreamType.EVENT_NOTIFICATION;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = super.toJSON();
        jsonObject.put(EVENT_ID_KEY, eventId);
        jsonObject.put(CITY_KEY, city.name());
        return jsonObject;
    }

    public void launch(BaseContextActivity activity) {
        activity.reportActionToAnalytics("streamItemClick", eventId);
        activity.showEventDetails(EventsHighEndpoints.getEventDetailsURI(city, eventId),
                EventNotificationStreamItem.class.getSimpleName());
    }
}
