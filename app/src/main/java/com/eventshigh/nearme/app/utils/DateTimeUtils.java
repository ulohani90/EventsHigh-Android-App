package com.eventshigh.nearme.app.utils;

import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.data.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Helper methods for managing date and time.
 */
public class DateTimeUtils {
    public static class EventTime {
        public final String day;
        public final String date;
        public final
        @Nullable
        String time;

        public EventTime(String day, String date, @Nullable String time) {
            this.day = day;
            this.date = date;
            this.time = time;
        }

        public String toString() {
            return day + ", " + date + (time == null ? "" : " at " + time);
        }

        public String getDate() {
            return day + ", " + date;
        }

        @Override
        public boolean equals(Object o) {
            if (((EventTime) o).date.equalsIgnoreCase(this.date) && ((EventTime) o).day.equalsIgnoreCase(this.day) && ((EventTime) o).time.equalsIgnoreCase(this.time))
                return true;
            return false;
        }
    }

    private static final SimpleDateFormat FULL_DATE_TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);

    private static final SimpleDateFormat FULL_DATE_TIME_MILLIS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static final SimpleDateFormat DD_MM_YYYY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");


    public static
    @Nullable
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


    public static int getDaysLater(Event event) {
        Date eventDate = DateTimeUtils.getEventDate(event, 0);
        Date today = DateTimeUtils.toMidnight(Calendar.getInstance(), event.city.timeZone).getTime();
        return (int) TimeUnit.MILLISECONDS.toDays(eventDate.getTime() - today.getTime());
    }

    public static Date getEventDate(Event event, int occurrenceNo) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(event.eventTimings[occurrenceNo]));
        return toMidnight(cal, event.city.timeZone).getTime();
    }


    public static Calendar toMidnight(Calendar cal, @Nullable String timeZone) {
        if (timeZone != null) {
            cal.setTimeZone(TimeZone.getTimeZone(timeZone));
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static final SimpleDateFormat SIMPLE_DAY_FORMAT = new SimpleDateFormat("EE", Locale.US);
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("d MMM", Locale.US);
    private static final SimpleDateFormat SIMPLE_TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.US);

    public static
    @Nullable
    EventTime getEventTime(Event event, int index) {
        if (index >= event.eventTimings.length) {
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

    public static
    @Nullable
    String getTimeString(Date date, TimeZone timeZone) {
        synchronized (SIMPLE_TIME_FORMAT) {
            SIMPLE_TIME_FORMAT.setTimeZone(timeZone);
            String time = SIMPLE_TIME_FORMAT.format(date);
            time = ZEROS.matcher(time).replaceAll("");
            return time.equals("0") || time.equals("12 am") ? null : time;
        }
    }

    private static final SimpleDateFormat QUERY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static String queryToTitle(String query) {
        try {
            Date date = QUERY_DATE_FORMAT.parse(query);
            synchronized (SIMPLE_DATE_FORMAT) {
                return SIMPLE_DAY_FORMAT.format(date) + " " + SIMPLE_DATE_FORMAT.format(date);
            }
        } catch (ParseException e) {
            return Utils.capitalize(query);
        }
    }

    private static final SimpleDateFormat BROWSE_DATE_FORMAT = new SimpleDateFormat("EEE-dd-MMM-yyyy", Locale.US);

    public static
    @Nullable
    String parseBrowseDate(String browseQuery) {
        try {
            return QUERY_DATE_FORMAT.format(BROWSE_DATE_FORMAT.parse(browseQuery));
        } catch (ParseException e) {
            // do nothing
        }

        return null;
    }

    public static
    @Nullable
    String toBrowseDate(String dateStr) {
        try {
            return BROWSE_DATE_FORMAT.format(QUERY_DATE_FORMAT.parse(dateStr));
        } catch (ParseException e) {
            // do nothing
        }
        return null;
    }

    private static final SimpleDateFormat BLOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public static Date parseBlogDate(String blogDateStr) throws ParseException {
        return BLOG_DATE_FORMAT.parse(blogDateStr);
    }

    public static long parseOfferTime(String time) {
        Date date = new Date();
        try {
            date = FULL_DATE_TIME_MILLIS_FORMAT.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date.getTime();
    }


    public static String getRemainingTime(long milliseconds) {
        StringBuilder builder = new StringBuilder();
        long timeDifference = milliseconds - System.currentTimeMillis();
        long days = timeDifference / 86400000;
        if (days > 0) {
            builder.append(days + ((days > 1) ? " Days " : " Day "));
            timeDifference = timeDifference % 86400000;
        }
        long hours = timeDifference / 3600000;
        if (hours > 0) {
            builder.append(hours + ((hours > 1) ? " Hours " : " Hour "));
            timeDifference = timeDifference % 3600000;
        }
        long minutes = timeDifference / 60000;
        if (minutes > 0) {
            builder.append(minutes + ((minutes > 0) ? " Mins " : " Min "));
        }

        return builder.toString();
    }


    public static String getPointAddedOnString(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        StringBuilder builder = new StringBuilder();
        builder.append(cal.get(Calendar.DAY_OF_MONTH));
        builder.append(" ");
        builder.append(new SimpleDateFormat("MMM").format(cal.getTime()));
        builder.append(" - ");
        builder.append(cal.get(Calendar.HOUR) < 10 ? "0" + cal.get(Calendar.HOUR) : cal.get(Calendar.HOUR));
        builder.append(":");
        builder.append(cal.get(Calendar.MINUTE) < 10 ? "0" + cal.get(Calendar.MINUTE) : cal.get(Calendar.MINUTE));
        builder.append(" ");
        builder.append(cal.get(Calendar.AM_PM) == Calendar.PM ? "PM" : "AM");
        return builder.toString();
    }

    public static String getMovieShowDate(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        StringBuilder builder = new StringBuilder();
        builder.append(cal.get(Calendar.DAY_OF_MONTH));
        builder.append(" ");
        builder.append(new SimpleDateFormat("MMM").format(cal.getTime()));
        builder.append(" ");
        builder.append(cal.get(Calendar.YEAR));
        return builder.toString();
    }

    public static long parseMovieTime(String time) {
        Date date = new Date();
        try {
            date = DD_MM_YYYY_FORMAT.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date.getTime();
    }


}
