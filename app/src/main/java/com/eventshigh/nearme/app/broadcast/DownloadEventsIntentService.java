package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Pair;

import com.android.volley.Request;
import com.android.volley.Request.Priority;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.NotificationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DownloadEventsIntentService extends IntentService {
    public static final String ACTION_DOWNLOAD_MY_EVENTS = "downloadMyEvents";
    public static final String ACTION_DOWNLOAD_WEEKEND_EVENTS = "downloadWeekendEvents";

    public DownloadEventsIntentService() {
        super("DownloadEventsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!Preferences.getInstance(getApplicationContext()).shouldNotifyWeekly()) {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
            return;
        }

        // TODO: may be get the user location from LocationClient
        EventsContext eventsContext = new EventsContext(null, EventsHighEndpoints.QUERY_WEEKEND);
        City lastCity = GcmRegistration.getInstance(this).getLastCity();
        if (lastCity != null) {
            eventsContext.changeLocation(lastCity.cityBounds.getCenter());
        }

        if (ACTION_DOWNLOAD_WEEKEND_EVENTS.equals(intent.getAction())) {
            EventCollectionRequest.submit(this, eventsContext, Priority.NORMAL,
                    false /* shouldBypassCache */, true /* includeWithoutLocation */,
                    new WeekendEventsListener(intent), new WeekendEventsErrorListener(intent));
        } else {
            new MyEventsRequest(this, eventsContext, Request.Priority.NORMAL,
                    false /* shouldBypassCache */, true /* includeWithoutLocation */,
                    new MyEventsListener(intent), new MyEventsErrorListener(intent)).execute();
        }
    }

    private class MyEventsErrorListener implements Response.ErrorListener {
        private final Intent intent;

        private MyEventsErrorListener(Intent intent) {
            this.intent = intent;
        }

        @Override
        public void onErrorResponse(VolleyError volleyError) {
            // TODO: This could happen when user is not connected. should we retry at some other point?
            // Should we switch to SyncAdapters ?
            if (!new Account(DownloadEventsIntentService.this).getFollowingInterests().isEmpty()) {
                showNotification(new HashSet<Event>(), intent, NotificationUtils.MY_EVENTS_NOTIFICATION_ID);
            } else {
                WakefulBroadcastReceiver.completeWakefulIntent(intent);
            }
        }
    }

    private class MyEventsListener implements Response.Listener<MyEventsRequest.MyEvents> {
        private final Intent intent;

        private MyEventsListener(Intent intent) {
            this.intent = intent;
        }

        @Override
        public void onResponse(MyEventsRequest.MyEvents pairs, boolean isIntermediate) {
            // Merge all events into one List and remove duplicates.
            Set<Event> eventSet = new HashSet<>();
            for (Pair<String, List<Event>> entry : pairs) {
                eventSet.addAll(entry.second);
            }
            showNotification(eventSet, intent, NotificationUtils.MY_EVENTS_NOTIFICATION_ID);
        }
    }

    private class WeekendEventsErrorListener implements Response.ErrorListener {
        private final Intent intent;

        private WeekendEventsErrorListener(Intent intent) {
            this.intent = intent;
        }

        @Override
        public void onErrorResponse(VolleyError volleyError) {
            // TODO: This could happen when user is not connected. should we retry at some other point?
            // Should we switch to SyncAdapters ?
            showNotification(new HashSet<Event>(), intent, NotificationUtils.WEEKEND_EVENTS_NOTIFICATION_ID);
        }
    }

    private class WeekendEventsListener implements Response.Listener<List<Event>> {
        private final Intent intent;

        private WeekendEventsListener(Intent intent) {
            this.intent = intent;
        }

        @Override
        public void onResponse(final List<Event> featuredEvents, boolean isIntermediate) {
            // Merge all events into one List and remove duplicates.
            Set<Event> eventSet = new HashSet<>(featuredEvents);
            showNotification(eventSet, intent, NotificationUtils.WEEKEND_EVENTS_NOTIFICATION_ID);
        }
    }

    private void showNotification(Set<Event> eventSet, Intent intent, int notificationId) {
        List<Event> events = new ArrayList<>(eventSet);
        Collections.sort(events, new EventComparator(null,
                EventsMarkerManager.getInstance(DownloadEventsIntentService.this)));
        if (events.size() == 1) {
            NotificationUtils.showNotificationAndReleaseWakeLock(
                    DownloadEventsIntentService.this, intent, events.get(0),
                    notificationId);
        } else {
            if (notificationId == NotificationUtils.WEEKEND_EVENTS_NOTIFICATION_ID) {
                NotificationUtils.showFeaturedEventsNotificationAndReleaseWakeLock(
                        DownloadEventsIntentService.this, events, intent);
            } else {
                NotificationUtils.showMyEventsNotificationAndReleaseWakeLock(
                        DownloadEventsIntentService.this, events, intent);
            }
        }
    }
}
