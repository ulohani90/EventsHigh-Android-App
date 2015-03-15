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
        if (Preferences.getInstance(getApplicationContext()).shouldNotifyFavourited()) {
            Event event = intent.getParcelableExtra(BUNDLE_EVENT_KEY);
            if (event != null) {
                // Notify user about the Event.
                NotificationUtils.showNotificationAndReleaseWakeLock(this, event, intent);

                // do not release the WakeLock, it will be done by
                // NotificationUtils.showNotificationAndReleaseWakeLock called above.
                return;
            }
        }

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        EventAlarmBroadcastReceiver.completeWakefulIntent(intent);
    }
}
