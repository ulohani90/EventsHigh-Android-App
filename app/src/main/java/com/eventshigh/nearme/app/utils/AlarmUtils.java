package com.eventshigh.nearme.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;

import com.eventshigh.nearme.app.broadcast.DownloadEventsBroadcastReceiver;
import com.eventshigh.nearme.app.broadcast.DownloadEventsIntentService.IntentType;
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


    public static void setWeeklyAlarms(Context context) {
        cancelOldMyEventsAlarm(context);
        cancelWeekendEventsAlarm(context);

        setMyEventsAlarm(context);
    }

    private static void setMyEventsAlarm(Context context) {
        // Set the alarm to start at 3pm-4pm on a random day except Friday and Monday.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS * 7);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 15);
        calendar.set(Calendar.MINUTE, new Random().nextInt(60));
        Intent intent = new Intent(context, DownloadEventsBroadcastReceiver.class);
        setRepeatingAlarm(context, intent, IntentType.MY_EVENTS.intentAction,
                calendar.getTimeInMillis(), 7);
    }

    private static void setRepeatingAlarm(Context context, Intent intent, String intentAction,
                                          long alarmTimeMillis, int daysInterval) {
        intent.setAction(intentAction);
        boolean isAlarmAlreadySet = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_NO_CREATE) != null;

        // If the alarm is already set, there is nothing to do
        if (isAlarmAlreadySet) {
            Log.i(LOG_TAG, intentAction + " alarm is already set");
            return;
        }

        Log.i(LOG_TAG, "Setting alarm at " +
                DateTimeUtils.timeToFullFormat(alarmTimeMillis) + " for " + intentAction);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, alarmTimeMillis,
                AlarmManager.INTERVAL_DAY * daysInterval, alarmIntent);
    }

    private static void cancelWeekendEventsAlarm(Context context) {
        Intent intent = new Intent(context, DownloadEventsBroadcastReceiver.class);
        intent.setAction(IntentType.WEEKEND_EVENTS.intentAction);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_NO_CREATE);
        if (alarmIntent != null) {
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmMgr.cancel(alarmIntent);
            Log.i(LOG_TAG, "Canceled old weekend events alarm");
        }
    }

    private static void cancelOldMyEventsAlarm(Context context) {
        Intent intent = new Intent(context, DownloadEventsBroadcastReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_NO_CREATE);
        if (alarmIntent != null) {
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmMgr.cancel(alarmIntent);
            Log.i(LOG_TAG, "Canceled old my events alarm");
        }
    }
}
