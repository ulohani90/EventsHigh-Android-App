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
    public static class EventTime {
        public final String day;
        public final String date;
        public final @Nullable String time;

        public EventTime(String day, String date, @Nullable String time) {
            this.day = day;
            this.date = date;
            this.time = time;
        }

        public String toString() {
            return day + " " + date + (time == null ? "" : ", " + time);
        }
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

        Date todayMidnight = toMidnight(Calendar.getInstance(), timeZone).getTime();
        Date mergedDate = FULL_DATE_TIME_FORMAT.parse(date.split(":")[0] + " " + time + " " + timeZone);
        return mergedDate.after(todayMidnight) ? mergedDate : null;
    }

    public static Date getEventDate(Event event, int occurrenceNo) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(event.eventTimings[occurrenceNo]));
        return toMidnight(cal, event.city.timeZone).getTime();
    }

    public static Calendar toMidnight(Calendar cal, String timeZone) {
        cal.setTimeZone(TimeZone.getTimeZone(timeZone));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static final SimpleDateFormat SIMPLE_DAY_FORMAT = new SimpleDateFormat("EE");
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("d MMM");
    private static final SimpleDateFormat SIMPLE_TIME_FORMAT = new SimpleDateFormat("h:mm a");

    public static @Nullable EventTime getEventTime(Event event, int index) {
        if (event.eventTimings.length < index) {
            return null;
        }

        return dateToEventTime(new Date(event.eventTimings[index]),
                TimeZone.getTimeZone(event.city.timeZone));
    }

    public static EventTime dateToEventTime(Date date, TimeZone timeZone) {
        synchronized (SIMPLE_DATE_FORMAT) {
            SIMPLE_DAY_FORMAT.setTimeZone(timeZone);
            SIMPLE_DATE_FORMAT.setTimeZone(timeZone);
            return new EventTime(SIMPLE_DAY_FORMAT.format(date),
                    SIMPLE_DATE_FORMAT.format(date),
                    getTimeString(date, timeZone));
        }
    }

    private static final Pattern ZEROS = Pattern.compile(":00");
    public static @Nullable String getTimeString(Date date, TimeZone timeZone) {
        synchronized (SIMPLE_TIME_FORMAT) {
            SIMPLE_TIME_FORMAT.setTimeZone(timeZone);
            String time = SIMPLE_TIME_FORMAT.format(date);
            time = ZEROS.matcher(time).replaceAll("");
            return time.equals("0") || time.equals("12 am") ? null : time;
        }
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static String dateQueryToTitle(String dateStr) {
        try {
            Date date = DATE_FORMAT.parse(dateStr);
            synchronized (SIMPLE_DATE_FORMAT) {
                return SIMPLE_DAY_FORMAT.format(date) + " " + SIMPLE_DATE_FORMAT.format(date);
            }
        } catch (ParseException e) {
            return  dateStr;
        }
    }
}
