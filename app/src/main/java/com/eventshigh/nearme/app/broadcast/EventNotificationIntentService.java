package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Intent;

import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.NotificationUtils;

public class EventNotificationIntentService extends IntentService {
    public static final String BUNDLE_EVENT_KEY = "event";

    public EventNotificationIntentService() {
        super("EventNotificationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Event event = intent.getParcelableExtra(BUNDLE_EVENT_KEY);

        if (!Preferences.getInstance(getApplicationContext()).shouldNotifyFavourited() ||
            event == null) {
            // Release the wake lock provided by the WakefulBroadcastReceiver.
            EventAlarmBroadcastReceiver.completeWakefulIntent(intent);
            return;
        }

        // Notify user about the Event.
        NotificationUtils.showNotificationAndReleaseWakeLock(this, intent, event);
    }
}
