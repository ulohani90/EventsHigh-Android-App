package com.eventshigh.nearme.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.text.format.DateUtils;

import com.eventshigh.nearme.app.broadcast.EventNotificationIntentService;
import com.eventshigh.nearme.app.broadcast.GcmBroadcastReceiver;
import com.eventshigh.nearme.app.data.Event;

/**
 * Helper class to set and cancel alarms for an event.
 */
public class AlarmUtils {
    public static void setAlarm(Context context, Event event) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Parcel parcel = Parcel.obtain();
        event.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Intent intent = new Intent(context, GcmBroadcastReceiver.class);
        intent.putExtra(EventNotificationIntentService.BUNDLE_EVENT_KEY, parcel.marshall());
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context,
                event.hashCode(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
        // Setup an alarm 1 day before the event
        alarmMgr.set(
                AlarmManager.RTC_WAKEUP,
                event.eventTimings[0] - DateUtils.DAY_IN_MILLIS,
                alarmIntent);
        parcel.recycle();
    }

    public static void cancelAlarm(Context context, Event event) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, GcmBroadcastReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context,
                event.hashCode(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmMgr.cancel(alarmIntent);
    }
}
