package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.CustomUrlActivity;
import com.eventshigh.nearme.app.activity.FeedbackActivity;
import com.eventshigh.nearme.app.activity.LaunchActivity;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.data.stream.EventNotificationStreamItem;
import com.eventshigh.nearme.app.data.stream.QueryNotificationStreamItem;
import com.eventshigh.nearme.app.data.stream.TicketNotificationStreamItem;
import com.eventshigh.nearme.app.notification.EHNotification;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Account.UserInfo;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.ContactUtils;
import com.eventshigh.nearme.app.utils.GAHelper;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.utils.ZendeskUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

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
        @Nullable
        public final LatLng boundCenter;
        public final double radiusInMeter;

        private ParsedBundle (EHNotification EHNotification,
                              @Nullable LatLng boundCenter, double radiusInMeter) {
            this.EHNotification = EHNotification;
            this.boundCenter = boundCenter;
            this.radiusInMeter = radiusInMeter;
        }

        private boolean isInRadius(@Nullable LatLng location) {
            return location != null &&
                LocationUtils.distanceInMeters(location, boundCenter) < radiusInMeter;
        }

        private boolean isInRadius(@Nullable Location location) {
            return location != null && isInRadius(LocationUtils.locationToLatLng(location));
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

        UserContact contact = null;
        if (mobileNo != null) {
            UserInfo userInfo = new Account(this).getUserInfo();
            if (mobileNo.equals(userInfo.phoneNo)) {
                reportAction("notificationSkipped");
                return null;
            }
            contact = ContactUtils.getContactForServerPhone(this, mobileNo);
        }

        if (contact != null) {
            if (!(new FriendsStore(this)).isFollowing(contact.contactId)) {
                // Don't show notification if the user is not following this friend
                return null;
            }

            title = title.replace("Your friend", contact.name);
            message = message.replace("Your friend", contact.name);
        }

        if (eventId == null && query == null && contestUrl == null && ticket == null && target == null) {
            Log.w(LOG_TAG, "Invalid notification, nether eventId, query, ticket or contest param passed");
            return null;
        }

        double lat = 0 , lon = 0, distance = 0;
        boolean bounded = false;
        String boundsCombinedStr = msg.getString("bounds");
        if (boundsCombinedStr != null) {
            String[] boundsStr = boundsCombinedStr.split(",", 3);
            if (boundsStr.length == 3) {
                lat = Double.parseDouble(boundsStr[0]);
                lon = Double.parseDouble(boundsStr[1]);
                distance = Double.parseDouble(boundsStr[2]);
                bounded = true;
            }
        }

        PendingIntent contentIntent;
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
        } else if (ticket != null) {
            ZendeskUtils.initZendesk(this);
            Intent intent = new Intent(this, FeedbackActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
            TicketNotificationStreamItem.record(this, title, message, imageUrl, ticket);
        } else if (target != null) {
            Intent intent = null;
            if (target.startsWith("tab:")) {
                intent = new Intent(this, LaunchActivity.class);
                intent.putExtra(LaunchActivity.DEFAULT_TAB_PARAM, target.split(":", 2)[1]);
            } else {
                try {
                    intent = new Intent(this, getClassLoader().loadClass(target));
                } catch (ClassNotFoundException e) {
                    Crashlytics.getInstance().core.logException(e);
                }
            }

            if (intent == null) { return null; }
            intent.setAction(BaseActivity.NOTIFICATION_ACTION + target);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        } else {
            Intent intent = new Intent(this, CustomUrlActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION + title);
            intent.setData(Uri.parse(contestUrl));
            intent.putExtra(CustomUrlActivity.EXTRA_TITLE_KEY, title);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        EHNotification EHNotification =  new EHNotification(this, alarmIntent, title, message,
                imageUrl, contentIntent,
                priority == null ? Notification.PRIORITY_LOW : Notification.PRIORITY_HIGH,
                contact
        );
        return new ParsedBundle(EHNotification, bounded ? new LatLng(lat, lon) : null, distance);
    }

    private void sendNotification(@Nullable ParsedBundle parsedBundle, Intent wakeupIntent) {
        if (parsedBundle != null) {
            if (parsedBundle.boundCenter != null) {
                new BoundsVerifier(parsedBundle, wakeupIntent).checkAndNotify();
            } else {
                parsedBundle.EHNotification.showNotificationAndReleaseWakeLock();
            }
        } else {
            // Release the wake lock provided by the WakefulBroadcastReceiver.
            reportAction("notificationSkipped");
            GcmBroadcastReceiver.completeWakefulIntent(wakeupIntent);
        }
    }

    private  class BoundsVerifier {
        private final ParsedBundle parsedBundle;
        private final Intent wakeupIntent;
        private GoogleApiClient client;
        private boolean completeWakefulIntentCalled = false;

        private BoundsVerifier(ParsedBundle parsedBundle, Intent wakeupIntent) {
            this.parsedBundle = parsedBundle;
            this.wakeupIntent = wakeupIntent;
        }

        private void checkAndNotify() {
            client = new GoogleApiClient.Builder(GcmIntentService.this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Location location = LocationServices.FusedLocationApi.getLastLocation(client);
                        if (parsedBundle.isInRadius(location)) {
                            parsedBundle.EHNotification.showNotificationAndReleaseWakeLock();
                        } else {
                            reportAction("notificationSkippedOutOfBounds");
                        }

                        client.disconnect();
                        completeWakefulIntent();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        completeWakefulIntent();
                    }
                })
                    .addOnConnectionFailedListener(new OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {
                            completeWakefulIntent();
                        }
                })
                .build();

            client.connect();
        }

        private synchronized void completeWakefulIntent() {
            if (!completeWakefulIntentCalled) {
                GcmBroadcastReceiver.completeWakefulIntent(wakeupIntent);
                completeWakefulIntentCalled = true;
            }
        }
    }
}
