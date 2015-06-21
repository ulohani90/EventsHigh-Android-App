package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.format.DateUtils;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.LaunchActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.NotificationUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyEventsNotificationService extends IntentService {
    private static final String PREF_FILE_NAME = "my_events_notification_data";
    private static final String PARAM_LAST_NOTIFICATION_TIMESTAMP = "last_notification_timestamp";

    public MyEventsNotificationService() {
        super("MyEventsNotificationService");
    }

    private SharedPreferences preferences;

    @Override
    protected void onHandleIntent(Intent wakefulIntent) {
        // Read the preferences which is used to store the last notification time.
        preferences = getSharedPreferences(PREF_FILE_NAME, 0);
        long lastNotificationTime = preferences.getLong(PARAM_LAST_NOTIFICATION_TIMESTAMP, 0);

        // Do not show notification if it was shown in last three days or it has shown for this week
        if (lastNotificationTime > 0) {
            Calendar lastNotificationCal = Calendar.getInstance();
            lastNotificationCal.setTimeInMillis(lastNotificationTime);
            lastNotificationCal.setFirstDayOfWeek(Calendar.SUNDAY);

            Calendar now = Calendar.getInstance();
            now.setFirstDayOfWeek(Calendar.SUNDAY);

            if (lastNotificationTime > System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * 3 ||
                now.get(Calendar.WEEK_OF_YEAR) == lastNotificationCal.get(Calendar.WEEK_OF_YEAR)) {
                WakefulBroadcastReceiver.completeWakefulIntent(wakefulIntent);
                return;
            }
        }

        EventsContext eventsContext = new EventsContext(null, "");
        City lastCity = GcmRegistration.getInstance(this).getLastCity();
        if (lastCity == null) {
            lastCity = City.BANGALORE;
        }
        eventsContext.changeLocation(lastCity.cityBounds.getCenter());

        new MyEventsRequest(this, eventsContext, Request.Priority.NORMAL, this,
                false /* shouldBypassCache */, true /* includeWithoutLocation */,
                new MyEventsListener(wakefulIntent), new MyEventsErrorListener(wakefulIntent)).execute();
    }

    private class MyEventsErrorListener implements Response.ErrorListener {
        private final Intent wakefulIntent;

        private MyEventsErrorListener(Intent wakefulIntent) {
            this.wakefulIntent = wakefulIntent;
        }

        @Override
        public void onErrorResponse(VolleyError volleyError) {
            // TODO: This could happen when user is not connected. should we retry at some other point?
            WakefulBroadcastReceiver.completeWakefulIntent(wakefulIntent);
        }
    }

    private class MyEventsListener implements Response.Listener<List<TopicEvents>> {
        private final Intent wakefulIntent;

        private MyEventsListener(Intent wakefulIntent) {
            this.wakefulIntent = wakefulIntent;
        }

        @Override
        public void onResponse(List<TopicEvents> myEvents, boolean isIntermediate) {
            // Merge all events into one List and remove duplicates.
            Set<Event> eventSet = new HashSet<>();
            for (TopicEvents topicEvents : myEvents) {
                if (!topicEvents.topicName.equalsIgnoreCase(MyEventsRequest.FAVOURITES_NAME)) {
                    eventSet.addAll(topicEvents.events);
                }
            }
            showNotification(eventSet, wakefulIntent);
        }
    }

    private void showNotification(Set<Event> eventSet, Intent wakefulIntent) {
        // Sort the events for user.
        List<Event> events = new ArrayList<>(eventSet);
        Collections.sort(events, new EventComparator(null));

        if (events.size() > 0) {
            preferences.edit().putLong(
                    PARAM_LAST_NOTIFICATION_TIMESTAMP, System.currentTimeMillis()).apply();
        }

        // In case of single event, use Single Event Notification stack.
        if (events.size() == 1) {
            NotificationUtils.showNotificationAndReleaseWakeLock(this, wakefulIntent, events.get(0),
                    NotificationUtils.MY_EVENTS_NOTIFICATION_ID);
            return;
        }

        // Build the notification information.
        Intent launchIntent = new Intent(this, LaunchActivity.class);
        launchIntent.setAction(BaseActivity.NOTIFICATION_ACTION + wakefulIntent.getAction());
        launchIntent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT,
                new EventsContext(null, EventsHighEndpoints.QUERY_MY_EVENT));
        NotificationUtils.showEventsNotification(this, NotificationUtils.MY_EVENTS_NOTIFICATION_ID,
                events, launchIntent, R.string.ui_my_events_message);

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        WakefulBroadcastReceiver.completeWakefulIntent(wakefulIntent);
    }
}
