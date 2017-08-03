package com.eventshigh.nearme.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.eventshigh.nearme.app.broadcast.EventAlarmBroadcastReceiver;
import com.eventshigh.nearme.app.broadcast.EventNotificationIntentService;
import com.eventshigh.nearme.app.data.Event;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to set and cancel alarms for an event.
 */
public class AlarmUtils {
    private static String LOG_TAG = AlarmUtils.class.getName();

    public static void setEventAlarm(Context context, Event event) {
        // find the upcoming event time.
        long now = System.currentTimeMillis();
        long upcomingEventTime = 0;
        for (long eventTime : event.eventTimings) {
            if (eventTime > now) {
                upcomingEventTime = eventTime;
                break;
            }
        }

        // No alarm if upcoming time is in next four hours.
        if (upcomingEventTime < now + TimeUnit.HOURS.toMillis(4)) {
            return;
        }

        // Set the alarm for 2 hours before the event if its happening in next 48 hours. otherwise
        // we will set the alarm for 1 day before the event.
        long alarmTimeMillis = (upcomingEventTime < now + TimeUnit.DAYS.toMillis(2)) ?
                upcomingEventTime - TimeUnit.HOURS.toMillis(2) :
                upcomingEventTime - TimeUnit.DAYS.toMillis(1);

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventAlarmBroadcastReceiver.class);
        intent.putExtra(EventNotificationIntentService.BUNDLE_EVENT_ID_KEY, event.id);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, event.hashCode(), intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Log.i(LOG_TAG, "Setting alarm at " + new Date(alarmTimeMillis) + " for " + event.title);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, alarmTimeMillis, alarmIntent);
    }

    public static void cancelEventAlarm(Context context, Event event) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventAlarmBroadcastReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context,
                event.hashCode(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmMgr.cancel(alarmIntent);
    }


}