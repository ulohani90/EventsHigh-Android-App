package com.eventshigh.nearme.app.broadcast;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcel;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.CustomUrlActivity;
import com.eventshigh.nearme.app.activity.EventDetailActivity;
import com.eventshigh.nearme.app.activity.LaunchActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.settings.Preferences;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
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

    public static final int NOTIFICATION_ID = 1;
    public static final String BUNDLE_EVENT_KEY = "event";

    private GoogleApiClient client;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.

        Bundle extras = intent.getExtras();
        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't recognize.
             */
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
            String messageType = gcm.getMessageType(intent);
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                // Post notification of received message.
                sendNotification(extras);
            }

            byte[] byteArrayExtra = intent.getByteArrayExtra(BUNDLE_EVENT_KEY);
            if (byteArrayExtra != null) {
                Parcel parcel = Parcel.obtain();
                parcel.unmarshall(byteArrayExtra, 0, byteArrayExtra.length);
                parcel.setDataPosition(0);
                Event event = Event.CREATOR.createFromParcel(parcel);
                showNotification(event);
                parcel.recycle();
            }
        }

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void showNotification(Event event) {
        CharSequence relativeTime = DateUtils.getRelativeDateTimeString(
                getApplicationContext(), event.eventTimings[0],
                DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        final Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(event.title)
                .setContentText(relativeTime)
                .setAutoCancel(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_RECOMMENDATION)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(relativeTime))
//                .setContentIntent(contentIntent)
                .build();
        showNotification(notification);
    }

    // Put the message into a notification and post it.
    private void sendNotification(Bundle msg) {
        String title = Utils.checkIfUnknown(msg.getString("t"));
        String message = Utils.checkIfUnknown(msg.getString("m"));
        if (message == null || title == null) {
            Log.w(LOG_TAG, "Invalid notification: message: " + message + ", title: " + title);
            return;
        }

        String eventId = Utils.checkIfUnknown(msg.getString("id"));
        String query = Utils.checkIfUnknown(msg.getString("q"));
        String contestUrl = Utils.checkIfUnknown(msg.getString("contest"));
        if (eventId == null && query == null && contestUrl == null) {
            Log.w(LOG_TAG, "Invalid notification, nether eventId, query or contest param passed");
            return;
        }

        Preferences preferences = Preferences.getInstance(getApplicationContext());
        if ((eventId != null && !preferences.shouldNotifyNearBy()) ||
            (query != null && !preferences.shouldNotifyWeekend())) {
            Log.w(LOG_TAG, "notification skipped as per user preference");
            return;
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
            City city = gcmRegistration.getLastCity();
            if (city == null) {
                // placeholder for city.
                city = City.BANGALORE;
            }

            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION);
            intent.setData(EventsHighEndpoints.getEventDetailsURI(city, eventId));
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        } else if (query != null) {
            Intent intent = new Intent(this, LaunchActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION);
            intent.setData(EventsHighEndpoints.getWebUri(new EventsContext(null, query)));
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        } else {
            Intent intent = new Intent(this, CustomUrlActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION);
            intent.putExtra(CustomUrlActivity.EXTRA_TITLE_KEY, title);
            intent.putExtra(CustomUrlActivity.EXTRA_URL_KEY, contestUrl);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        @SuppressLint("InlinedApi")
        final Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_RECOMMENDATION)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(contentIntent)
                .build();

        if (!bounded) {
            showNotification(notification);
        } else {
            final LatLng center = new LatLng(lat, lon);
            final double radius = distance;
            client = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Location location = LocationServices.FusedLocationApi.getLastLocation(client);
                        if (location == null ||
                            LocationUtils.distanceInMeters(
                                    LocationUtils.locationToLatLng(location), center) > radius) {
                            Log.w(LOG_TAG, "notification skipped, user location: " + location);
                        } else {
                            showNotification(notification);
                        }

						client.disconnect();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        // do nothing
                    }
                })
                .build();
            client.connect();
        }
    }

    private void showNotification(Notification notification) {
        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}
