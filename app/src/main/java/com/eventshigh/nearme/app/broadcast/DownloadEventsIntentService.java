package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.android.volley.Request;
import com.android.volley.Request.Priority;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.LaunchActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvent;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.NotificationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DownloadEventsIntentService extends IntentService {

    public enum IntentType {
        MY_EVENTS("eh_my_events", EventsHighEndpoints.QUERY_MY_EVENT,
                R.string.ui_upcoming_events, R.string.ui_upcoming_events_msg,
                NotificationUtils.MY_EVENTS_NOTIFICATION_ID),
        WEEKEND_EVENTS("eh_weekend_events", EventsHighEndpoints.QUERY_WEEKEND,
                R.string.ui_weekend_events, R.string.ui_weekend_events_msg,
                NotificationUtils.WEEKEND_EVENTS_NOTIFICATION_ID);

        public final String intentAction;
        public final String query;
        public final int titleResId;
        public final int messageRedId;
        public final int notificationId;

        IntentType(String intentAction, String query, int titleResId, int messageRedId,
                   int notificationId) {
            this.intentAction = intentAction;
            this.query = query;
            this.titleResId = titleResId;
            this.messageRedId = messageRedId;
            this.notificationId = notificationId;
        }

        public static IntentType getType(Intent intent) {
            if (intent.getAction().equals(WEEKEND_EVENTS.intentAction)) {
                return WEEKEND_EVENTS;
            }

            return MY_EVENTS;
        }
    }

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
        IntentType type = IntentType.getType(intent);
        EventsContext eventsContext = new EventsContext(null, type.query);
        City lastCity = GcmRegistration.getInstance(this).getLastCity();
        if (lastCity != null) {
            eventsContext.changeLocation(lastCity.cityBounds.getCenter());
        }

        if (type == IntentType.WEEKEND_EVENTS) {
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
                showNotification(new HashSet<Event>(), intent);
            } else {
                WakefulBroadcastReceiver.completeWakefulIntent(intent);
            }
        }
    }

    private class MyEventsListener implements Response.Listener<MyEvents> {
        private final Intent intent;

        private MyEventsListener(Intent intent) {
            this.intent = intent;
        }

        @Override
        public void onResponse(MyEvents myEvents, boolean isIntermediate) {
            // Merge all events into one List and remove duplicates.
            Set<Event> eventSet = new HashSet<>();
            for (TopicEvent topicEvent : myEvents.topicEvents) {
                eventSet.addAll(topicEvent.events);
            }
            showNotification(eventSet, intent);
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
            showNotification(new HashSet<Event>(), intent);
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
            showNotification(eventSet, intent);
        }
    }

    private void showNotification(Set<Event> eventSet, Intent intent) {
        // Sort the events for user.
        List<Event> events = new ArrayList<>(eventSet);
        Collections.sort(events, new EventComparator(null,
                EventsMarkerManager.getInstance(DownloadEventsIntentService.this)));

        // In case of single event, use Single Event Notification stack.
        IntentType type = IntentType.getType(intent);
        if (events.size() == 1) {
            NotificationUtils.showNotificationAndReleaseWakeLock(this, intent, events.get(0),
                    type.notificationId);
            return;
        }

        // Build the notification information.
        Intent launchIntent = new Intent(this, LaunchActivity.class);
        launchIntent.setAction(BaseActivity.NOTIFICATION_ACTION + intent.getAction());
        launchIntent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, new EventsContext(null, type.query));
        NotificationUtils.showEventsNotification(this, type.notificationId, events, launchIntent,
                type.titleResId, type.messageRedId);

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }
}
