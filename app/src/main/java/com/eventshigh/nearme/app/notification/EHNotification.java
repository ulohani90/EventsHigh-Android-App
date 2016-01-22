package com.eventshigh.nearme.app.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.ImageView.ScaleType;

import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.RequestFuture;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.network.VolleyHelper;
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

    public final @Nullable UserContact contact;

    public EHNotification(Context context, String title, String message, String imageUrl,
            PendingIntent launchIntent, int priority, @Nullable UserContact contact) {
        this.context = context;
        this.notificationId = contact == null ? GCM_NOTIFICATION_ID : contact.mobileNo.hashCode();
        this.priority = priority;

        this.title = title;
        this.message = message;
        this.imageUrl = imageUrl;
        this.launchIntent = launchIntent;
        this.contact = contact;
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
            } catch (Exception e) {
                Crashlytics.getInstance().core.logException(e);
            }
        }

        // Show notification.
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        GAHelper.getInstance(context).reportActionToAnalytics("background", "notificationShown",
                title);
        notificationManager.notify(notificationId, createNotification(bitmap));
    }

    @SuppressLint("InlinedApi")
    private Notification createNotification(@Nullable Bitmap bitmap) {
        Bitmap largeIcon = null;
        if (contact != null) {
            largeIcon = UserContact.getPhotoForContactId(context, contact.contactId);
        }
        if (largeIcon == null) {
            largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_EVENT)
                .setPriority(priority)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setLargeIcon(largeIcon)
                .setContentIntent(launchIntent);

        if (priority >= Notification.PRIORITY_DEFAULT) {
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
}
