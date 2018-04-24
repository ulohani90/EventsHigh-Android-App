package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.RequestFuture;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.network.EventRequest;
import com.eventshigh.nearme.app.notification.EHNotification;

/**
 * Created by umesh on 26/04/16.
 */
public class EventNotificationIntentService extends IntentService {
    public static final String BUNDLE_EVENT_KEY = "event";
    public static final String BUNDLE_EVENT_ID_KEY = "event";
    private static int FOREGROUND_ID=1339;

    public EventNotificationIntentService() {
        super("EventNotificationIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(FOREGROUND_ID, buildForegroundNotification(createNotificationChannel()));
        }
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

    private Notification buildForegroundNotification(String channelId) {
        NotificationCompat.Builder b=new NotificationCompat.Builder(this);

        b.setOngoing(true)
                .setChannelId(channelId)
                .setContentTitle("Updating")
                .setContentText("")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setTicker("Events High");

        return(b.build());
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(){
        String channelId = "eh_app_notification_intent_service";
        String channelName = "Notification Background Service";
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(chan);
        return channelId;
    }
}