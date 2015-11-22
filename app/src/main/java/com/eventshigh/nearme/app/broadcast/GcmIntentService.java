package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BlogEntryActivity;
import com.eventshigh.nearme.app.activity.CustomUrlActivity;
import com.eventshigh.nearme.app.activity.LaunchActivity;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.stream.EventNotificationStreamItem;
import com.eventshigh.nearme.app.data.stream.QueryNotificationStreamItem;
import com.eventshigh.nearme.app.notification.EHNotification;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.GAHelper;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * See {@link com.eventshigh.nearme.app.broadcast.GcmBroadcastReceiver} for details.
 *
 * See https://developer.android.com/google/gcm/client.html.
 */
public class GcmIntentService extends IntentService {
    private static final String LOG_TAG = GcmIntentService.class.getSimpleName();

    private GAHelper gaHelper;
    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(final Intent wakeupIntent) {
        // Filter messages based on message type. Since it is likely that GCM will be extended
        // in the future with new message types, just ignore any message types you're not
        // interested in, or that you don't recognize.
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(wakeupIntent);
        if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            gaHelper = GAHelper.getInstance(GcmIntentService.this);

            ParsedBundle parsedBundle = parseBundle(wakeupIntent.getExtras(), wakeupIntent);
            sendNotification(parsedBundle, wakeupIntent);
        }
    }

    private void reportAction(String actionName) {
        gaHelper.reportActionToAnalytics(LOG_TAG, actionName);
    }

    private static class ParsedBundle {
        public final EHNotification EHNotification;
        public final boolean isBounded;

        private ParsedBundle (EHNotification EHNotification, boolean isBounded) {
            this.EHNotification = EHNotification;
            this.isBounded = isBounded;
        }
    }

    private @Nullable ParsedBundle parseBundle(Bundle msg, Intent alarmIntent) {
        String title = Utils.checkIfUnknown(msg.getString("t"));
        String message = Utils.checkIfUnknown(msg.getString("m"));
        if (message == null || title == null) {
            Log.w(LOG_TAG, "Invalid notification: message: " + message + ", title: " + title);
            return null;
        }

        String eventId = Utils.checkIfUnknown(msg.getString("id"));
        String query = Utils.checkIfUnknown(msg.getString("q"));
        String contestUrl = Utils.checkIfUnknown(msg.getString("contest"));
        String imageUrl = Utils.checkIfUnknown(msg.getString("img"));
        String ticket = Utils.checkIfUnknown(msg.getString("ticket"));
        String target = Utils.checkIfUnknown(msg.getString("target"));
        String priority = Utils.checkIfUnknown(msg.getString("priority"));
        String mobileNo = Utils.checkIfUnknown(msg.getString("mobile"));

        if (eventId == null && query == null && contestUrl == null && ticket == null && target == null) {
            Log.w(LOG_TAG, "Invalid notification, nether eventId, query, ticket or contest param passed");
            return null;
        }

        boolean bounded = false;
        String boundsCombinedStr = msg.getString("bounds");
        if (boundsCombinedStr != null) {
            String[] boundsStr = boundsCombinedStr.split(",", 3);
            if (boundsStr.length == 3) {
                bounded = true;
            }
        }

        PendingIntent contentIntent = null;
        if (eventId != null) {
            GcmRegistration gcmRegistration = GcmRegistration.getInstance(getApplicationContext());
            contentIntent = EHNotification.createPendingIntent(this, eventId,
                    gcmRegistration.getLastCity());
            EventNotificationStreamItem.record(this, title, message, imageUrl, mobileNo, eventId,
                    gcmRegistration.getLastCity());
        } else if (query != null) {
            Intent intent = new Intent(this, LaunchActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION + query);
            intent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, new EventsContext(null, query));
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
            QueryNotificationStreamItem.record(this, title, message, imageUrl, mobileNo, query);
        } else if (target != null) {
            Intent intent = null;
            if (target.startsWith("tab:")) {
                intent = new Intent(this, LaunchActivity.class);
                intent.putExtra(LaunchActivity.DEFAULT_TAB_PARAM, target.split(":", 2)[1]);
            } else {
                try {
                    intent = new Intent(this, getClassLoader().loadClass(target));
                } catch (ClassNotFoundException e) {
                    // Ignore.
                }
            }

            if (intent == null) { return null; }
            intent.setAction(BaseActivity.NOTIFICATION_ACTION + target);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        } else if (contestUrl != null) {
            Intent intent = new Intent(this,
                contestUrl.contains(CustomUrlActivity.BLOG_HOST) ? BlogEntryActivity.class : CustomUrlActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION + title);
            intent.setData(Uri.parse(contestUrl));
            intent.putExtra(CustomUrlActivity.EXTRA_TITLE_KEY, title);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        if (contentIntent != null) {
            EHNotification EHNotification = new EHNotification(this, alarmIntent, title, message,
                    imageUrl, contentIntent,
                    priority == null ? Notification.PRIORITY_LOW : Notification.PRIORITY_HIGH);
            return new ParsedBundle(EHNotification, bounded);
        } else {
            return null;
        }
    }

    private void sendNotification(@Nullable ParsedBundle parsedBundle, Intent wakeupIntent) {
        if (parsedBundle != null && parsedBundle.isBounded) {
            parsedBundle.EHNotification.showNotificationAndReleaseWakeLock();
        } else {
            // Release the wake lock provided by the WakefulBroadcastReceiver.
            reportAction("notificationSkipped");
            GcmBroadcastReceiver.completeWakefulIntent(wakeupIntent);
        }
    }
}
