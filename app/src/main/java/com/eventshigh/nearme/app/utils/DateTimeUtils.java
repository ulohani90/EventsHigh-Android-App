package com.eventshigh.nearme.app.utils;

import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.data.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Helper methods for managing date and time.
 */
public class DateTimeUtils {
    /**
     * Get the date for give dayItemNo.
     *   dayItemNo 0: Today
     *   dayItemNo 1: Tomorrow
     *   and so on.
     */
    public static Date getDate(int dayItemNo) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, dayItemNo);
        return cal.getTime();
    }

    private static final SimpleDateFormat FULL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static String getDateString(Date date) {
        return FULL_DATE_FORMAT.format(date);
    }

    private static final SimpleDateFormat FULL_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    public static @Nullable
    Date mergeDateTime(String date, String time, String timeZone) throws ParseException {
        if (date == null || date.isEmpty() || date.equalsIgnoreCase("null")) {
            return null;
        }

        if (time == null || time.isEmpty() || time.equalsIgnoreCase("null") ||
                time.startsWith("01:02") || time.startsWith("01:01")) {
            time = "00:00:00";
        }

        if (time.split(":").length == 2) {
            time = time + ":00";
        }

        return FULL_DATE_TIME_FORMAT.parse(date.split(":")[0] + " " + time + " " + timeZone);
    }

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("MMM d");
    private static final SimpleDateFormat SIMPLE_TIME_FORMAT = new SimpleDateFormat("h:mm a");
    private static final Pattern ZEROS = Pattern.compile(":00");
    public static @Nullable String getEventTime(Event event, boolean includeDate) {
        if (event.eventTimings.length == 0) {
            return null;
        }

        TimeZone timeZone =  TimeZone.getTimeZone(event.city.timeZone);
        Date eventTime = new Date(event.eventTimings[0]);
        String timeString = getTimeString(eventTime, timeZone);
        if (!includeDate) {
            return timeString;
        }

        synchronized (SIMPLE_DATE_FORMAT) {
            SIMPLE_DATE_FORMAT.setTimeZone(timeZone);
            return SIMPLE_DATE_FORMAT.format(eventTime) + (timeString == null ? "" : ", " + timeString);
        }
    }

    public static @Nullable String getTimeString(Date date, TimeZone timeZone) {
        synchronized (SIMPLE_TIME_FORMAT) {
            SIMPLE_TIME_FORMAT.setTimeZone(timeZone);
            String time = SIMPLE_TIME_FORMAT.format(date);
            time = ZEROS.matcher(time).replaceAll("");
            return time.equals("0") || time.equals("12 am") ? null : time;
        }
    }
}
