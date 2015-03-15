package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Pair;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.NotificationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        // TODO: may be get the user location from LocationClient
        City lastCity = GcmRegistration.getInstance(this).getLastCity();
        if (lastCity != null) {
            location = lastCity.cityBounds.getCenter();
        }
        EventsContext eventsContext = new EventsContext(location, "this week");

        new MyEventsRequest(this, eventsContext, Request.Priority.IMMEDIATE,
                false /* shouldBypassCache */, true /* includeWithoutLocation */,
                this, this).execute();
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        // TODO: This could happen when user is not connected. should we retry at some other point?
        // Should we switch to SyncAdapters ?
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }

    @Override
    public void onResponse(MyEventsRequest.MyEvents pairs, boolean isIntermediate) {
        // Merge all events into one List and remove duplicates.
        Set<Event> eventSet = new HashSet<>();
        for (Pair<String, List<Event>> entry : pairs) {
            eventSet.addAll(entry.second);
        }
        List<Event> events = new ArrayList<>(eventSet);
        Collections.sort(events, new EventComparator(null, EventsMarkerManager.getInstance(this)));

        if (events.size() == 1) {
            NotificationUtils.showNotificationAndReleaseWakeLock(this, events.get(0), intent);
        } else if (events.size() > 1) {
            NotificationUtils.showNotificationAndReleaseWakeLock(this, events, intent);
        } else {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }
}
