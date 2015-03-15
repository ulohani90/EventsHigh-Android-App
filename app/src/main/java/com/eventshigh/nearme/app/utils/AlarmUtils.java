package com.eventshigh.nearme.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;

import com.eventshigh.nearme.app.broadcast.DownloadEventsBroadcastReceiver;
import com.eventshigh.nearme.app.broadcast.EventAlarmBroadcastReceiver;
import com.eventshigh.nearme.app.broadcast.EventNotificationIntentService;
import com.eventshigh.nearme.app.data.Event;

import java.util.Calendar;
import java.util.Random;

/**
 * Helper class to set and cancel alarms for an event.
 */
public class AlarmUtils {
    private static String LOG_TAG = AlarmUtils.class.getName();

    public static void setAlarm(Context context, Event event) {
        // Don't set an alarm if the event is going to happen within 1 day
        if (event.eventTimings[0] - System.currentTimeMillis() < DateUtils.DAY_IN_MILLIS) {
            Log.i(LOG_TAG, "Not setting alarm for " + event.title);
            return;
        }

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventAlarmBroadcastReceiver.class);
        intent.putExtra(EventNotificationIntentService.BUNDLE_EVENT_KEY, event);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, event.hashCode(), intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // Setup an alarm 1 day before the event
        long alarmTimeMillis = event.eventTimings[0] - DateUtils.DAY_IN_MILLIS;
        Log.i(LOG_TAG, "Setting alarm at " +
                DateTimeUtils.timeToFullFormat(alarmTimeMillis)  + " for " + event.title);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, alarmTimeMillis, alarmIntent);
    }

    public static void setMyEventsAlarm(Context context) {
        Intent intent = new Intent(context, DownloadEventsBroadcastReceiver.class);
        boolean isAlarmAlreadySet = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_NO_CREATE) != null;

        // If the alarm is already set, there is nothing to do
        if (isAlarmAlreadySet) {
            Log.i(LOG_TAG, "My events alarm is already set");
            return;
        }

        // Set the alarm to start at approximately between 10:00 a.m. and 2:00 p.m. to reduce load
        // on server (to make sure that not all devices contact the server at the same time).
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS);
        calendar.set(Calendar.HOUR_OF_DAY, 10 + new Random().nextInt(5));
        calendar.set(Calendar.MINUTE, new Random().nextInt(60));
        long alarmTimeMillis = calendar.getTimeInMillis();

        Log.i(LOG_TAG, "Setting alarm at " +
                DateTimeUtils.timeToFullFormat(alarmTimeMillis) + " for my events");
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, alarmTimeMillis,
                AlarmManager.INTERVAL_DAY, alarmIntent);
    }

    public static void cancelAlarm(Context context, Event event) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventAlarmBroadcastReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context,
                event.hashCode(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmMgr.cancel(alarmIntent);
    }
}
