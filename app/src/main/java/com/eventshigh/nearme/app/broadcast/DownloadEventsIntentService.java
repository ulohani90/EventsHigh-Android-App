package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.NotificationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class DownloadEventsIntentService extends IntentService
        implements Response.ErrorListener, Response.Listener<MyEventsRequest.MyEvents> {
    private Intent intent;
    private LatLng location;

    public DownloadEventsIntentService() {
        super("DownloadEventsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        this.intent = intent;

        // TODO(chandanp): may be get the user location from LocationClient
        EventsContext eventsContext = new EventsContext(null, "this week");
        City lastCity = GcmRegistration.getInstance(this).getLastCity();
        if (lastCity != null) {
            location = lastCity.cityBounds.getCenter();
            eventsContext.changeLocation(location);
        }

        new MyEventsRequest(this, eventsContext, Request.Priority.IMMEDIATE,
                true /* shouldBypassCache */, true /* includeWithoutLocation */,
                this, this).execute();
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }

    @Override
    public void onResponse(MyEventsRequest.MyEvents pairs, boolean b) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < pairs.size(); i++) {
            for (Event event : pairs.get(0).second) {
                events.add(event);
            }
        }

        if (events.size() == 1) {
            NotificationUtils.showNotificationAndReleaseWakeLock(this, events.get(0), intent);
        } else if (events.size() > 1) {
            NotificationUtils.showNotificationAndReleaseWakeLock(this, events, intent, location);
        } else {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }
}
