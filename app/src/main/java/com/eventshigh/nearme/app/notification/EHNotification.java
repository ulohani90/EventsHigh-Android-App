package com.eventshigh.nearme.app.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.format.DateUtils;
import android.widget.ImageView.ScaleType;

import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.RequestFuture;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.NewEventDetailActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.data.stream.EventNotificationStreamItem;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.GAHelper;
import com.eventshigh.nearme.app.utils.Utils;

/**
 * Represents the notification shown in EH.
 */
public class EHNotification {
    // See https://code.google.com/p/android/issues/detail?id=36744.
    public static final int NOTIFICATION_IMAGE_WIDTH_DP = 450;
    public static final int NOTIFICATION_IMAGE_HEIGHT_DP = 225;
    public static final int GCM_NOTIFICATION_ID = 1;

    public final Context context;

    public final String title;
    public final String message;
    public final String imageUrl;

    public final PendingIntent launchIntent;
    public final int notificationId;
    public final int priority;

    public final Intent wakefulIntent;

    public final
    @Nullable
    UserContact contact;

    public static final String NOTIFICATION_DELETED_ACTION = "notification_deleted_intent";


    public EHNotification(Context context, Intent wakefulIntent, Event event, int notificationId) {
        this.context = context;
        this.wakefulIntent = wakefulIntent;
        this.notificationId = notificationId;
        this.priority = Notification.PRIORITY_HIGH;

        title = event.title;
        CharSequence relativeTime = DateUtils.getRelativeDateTimeString(
                context, event.eventTimings.get(0),
                DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        message = String.format(
                context.getResources().getString(R.string.event_time_venue),
                relativeTime, event.getShortAddress());
        imageUrl = event.imgUrl;

        launchIntent = createPendingIntent(context, event.id, event.city);
        contact = null;

        // Record notification in stream.
        EventNotificationStreamItem.record(context, title, message, imageUrl, null, event.id,
                event.city);
    }


    public EHNotification(Context context, String title, String message, String imageUrl,
                          PendingIntent launchIntent, int priority,@Nullable int notificationId, @Nullable UserContact contact) {
        this.context = context;
        if(notificationId>0){
            this.notificationId = notificationId;
        }else{
            this.notificationId = contact == null ? GCM_NOTIFICATION_ID : contact.mobileNo.hashCode();
        }
        this.priority = priority;

        this.title = title;
        this.message = message;
        this.imageUrl = imageUrl;
        this.launchIntent = launchIntent;
        this.contact = contact;

        this.wakefulIntent = null;
    }


    public void showNotification() {
        // Submit the image request to Volley.
        Bitmap bitmap = null;
        if (imageUrl != null) {
            try {
                RequestFuture<Bitmap> imageBitmapFuture = RequestFuture.newFuture();
                ImageRequest request = new ImageRequest(imageUrl, imageBitmapFuture,
                        Utils.dpToPx(context, NOTIFICATION_IMAGE_WIDTH_DP),
                        Utils.dpToPx(context, NOTIFICATION_IMAGE_HEIGHT_DP),
                        ScaleType.CENTER_CROP,
                        null, imageBitmapFuture);
                VolleyHelper.addToRequestQueue(context, request);
                bitmap = imageBitmapFuture.get();
            } catch (Exception e){
                Crashlytics.getInstance().core.logException(e);
            }
        }

        // Show notification.
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        GAHelper.getInstance(context).reportActionToAnalytics("background", "notificationShown",
                title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("notify_001",
                    "Events High",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(notificationId, createNotification(bitmap));
        Preferences.getInstance(context).setIsNotificationActive(true);
        if (wakefulIntent != null)
            WakefulBroadcastReceiver.completeWakefulIntent(wakefulIntent);
    }



    @SuppressLint("InlinedApi")
    private Notification createNotification(@Nullable Bitmap bitmap){
        Bitmap largeIcon = null;
        if (contact != null) {
            largeIcon = UserContact.getPhotoForContactId(context, contact.contactId);
        }
        if (largeIcon == null) {
            largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        }

        Intent intent = new Intent(NOTIFICATION_DELETED_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "notify_001")
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_EVENT)
                .setPriority(priority)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setLargeIcon(largeIcon)
                .setContentIntent(launchIntent).setDeleteIntent(pendingIntent);



        if (!Preferences.getInstance(context).isNotificationActive()) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        }
        if (contact != null) {
            builder.addPerson("tel:" + contact.mobileNo);
        }

        if (bitmap != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .setSummaryText(message).bigPicture(bitmap).setBigContentTitle(title));
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }

        return builder.build();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(){
        String channelId = "eh_app_notification_service";
        String channelName = "Eh app notification service";
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.RED);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(chan);
        return channelId;
    }


    public static PendingIntent createPendingIntent(Context context, String eventId,
                                                     String city) {
        if (city == null) {
            // placeholder for city.
            city = City.BANGALORE.name();
        }
        Intent intent = new Intent(context, NewEventDetailActivity.class);
        intent.setAction(BaseActivity.NOTIFICATION_ACTION);
        intent.setData(EventsHighEndpoints.getEventDetailsURI(city, eventId));
        return PendingIntent.getActivity(context, 0, intent, 0);
    }


}
