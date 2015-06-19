package com.eventshigh.nearme.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;

import com.eventshigh.nearme.app.broadcast.EventAlarmBroadcastReceiver;
import com.eventshigh.nearme.app.broadcast.EventNotificationIntentService;
import com.eventshigh.nearme.app.broadcast.MyEventsAlarmReceiver;
import com.eventshigh.nearme.app.data.Event;

import java.util.Calendar;
import java.util.Random;

/**
 * Helper class to set and cancel alarms for an event.
 */
public class AlarmUtils {
    private static String LOG_TAG = AlarmUtils.class.getName();

    public static void setEventAlarm(Context context, Event event) {
        // Setup an alarm 1 day before the event.
        long alarmTimeMillis = event.eventTimings[0] - DateUtils.DAY_IN_MILLIS;

        // Don't set an alarm if the event is going to happen within 1 day
        if (alarmTimeMillis < System.currentTimeMillis()) {
            Log.i(LOG_TAG, "Not setting alarm for " + event.title);
            return;
        }

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventAlarmBroadcastReceiver.class);
        intent.putExtra(EventNotificationIntentService.BUNDLE_EVENT_KEY, event);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, event.hashCode(), intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Log.i(LOG_TAG, "Setting alarm at " +
                DateTimeUtils.timeToFullFormat(alarmTimeMillis)  + " for " + event.title);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, alarmTimeMillis, alarmIntent);
    }

    public static void cancelEventAlarm(Context context, Event event) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventAlarmBroadcastReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context,
                event.hashCode(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmMgr.cancel(alarmIntent);
    }

    // Set the alarm to start at 3pm-4pm on Sunday.
    public static void setMyEventsAlarm(Context context) {
        // Get the Intent for creating alarm.
        Intent intent = new Intent(context, MyEventsAlarmReceiver.class);
        intent.setAction("eh_my_events");

        // If the alarm is already set, there is nothing to do
        boolean isAlarmAlreadySet = PendingIntent.getBroadcast(context,
                NotificationUtils.MY_EVENTS_NOTIFICATION_ID, intent, PendingIntent.FLAG_NO_CREATE) != null;
        if (isAlarmAlreadySet) {
            Log.i(LOG_TAG, intent.getAction() + " alarm is already set");
            return;
        }

        // Get the instance of calendar for next Sunday.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 15);
        calendar.set(Calendar.MINUTE, new Random().nextInt(60));

        Log.i(LOG_TAG, "Setting alarm at " + calendar.getTime()  + " for " + intent.getAction());
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context,
                NotificationUtils.MY_EVENTS_NOTIFICATION_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);
    }
}
