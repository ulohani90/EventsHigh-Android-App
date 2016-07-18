package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;


import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.RequestFuture;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.network.EventRequest;
import com.eventshigh.nearme.app.notification.EHNotification;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by umesh on 26/04/16.
 */
public class EventNotificationIntentService extends IntentService {
    public static final String BUNDLE_EVENT_KEY = "event";
    public static final String BUNDLE_EVENT_ID_KEY = "event";

    public EventNotificationIntentService() {
        super("EventNotificationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent wakefulIntent) {
        Event event = null;
        try {
            event = wakefulIntent.getParcelableExtra(BUNDLE_EVENT_KEY);
        } catch (Exception e) {
            // event parsing can fail when app is updated and we add extra fields to events object.
            // ignore the exception, we can do nothing.
        }

        if (event == null) {
            String eventId = wakefulIntent.getParcelableExtra(BUNDLE_EVENT_ID_KEY);
            if (eventId != null) {
                RequestFuture<Event> eventFuture = RequestFuture.newFuture();
                EventRequest.submit(this, Uri.parse("https://www.eventshigh.com/detail/bangalore/" + eventId), Request.Priority.HIGH,
                        eventFuture, eventFuture);
                try {
                    event = eventFuture.get();
                } catch (Exception e) {
                    // Ignore.
                }
            }
        }

        if (event == null) {
            // Release the wake lock provided by the WakefulBroadcastReceiver.
            EventAlarmBroadcastReceiver.completeWakefulIntent(wakefulIntent);
            return;
        }

        // Notify user about the Event.
        EHNotification EHNotification = new EHNotification(this, wakefulIntent, event,
                event.hashCode());
        EHNotification.showNotification();
    }
}