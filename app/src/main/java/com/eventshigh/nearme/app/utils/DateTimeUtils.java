package com.eventshigh.nearme.app.utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventInfoObject;

import java.text.DateFormat;
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
    public static boolean isCurrentDate(long date) {
        Calendar c = Calendar.getInstance();


        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        if (date == c.getTime().getTime())
            return true;
        return false;
    }

    public static class EventTime implements Parcelable {
        public final String day;
        public final String date;
        public final
        @Nullable
        String time;
        public long longtime;

        public EventTime(String day, String date, @Nullable String time, long longtime) {
            this.day = day;
            this.date = date;
            this.time = time;
            this.longtime = longtime;
        }

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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(day);
            dest.writeString(date);
            dest.writeString(time);
            dest.writeLong(longtime);
        }

        // This is used to regenerate your object. All Parcelables must have
        // a CREATOR that implements these two methods
        public static final Parcelable.Creator<EventTime> CREATOR =
                new Parcelable.Creator<EventTime>() {
                    public EventTime createFromParcel(Parcel in) {
                        return new EventTime(in.readString(), in.readString(), in.readString(), in.readLong());
                    }

                    public EventTime[] newArray(int size) {
                        return new EventTime[size];
                    }

                };
    }

    public static final long MILLISECONDS_IN_A_DAY = 86400000;

    // private static final SimpleDateFormat FULL_DATE_TIME_MILLIS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    // private static final SimpleDateFormat DD_MM_YYYY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");


    public static
    @Nullable
    Date mergeDateTime(String date, String time, TimeZone timeZone) throws ParseException {

        SimpleDateFormat FULL_DATE_TIME_FORMAT =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);

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

        return FULL_DATE_TIME_FORMAT.parse(date.split(":")[0] + " " + time + " " + displayTimeZone(timeZone));
    }

    private static String displayTimeZone(TimeZone tz) {

        long hours = TimeUnit.MILLISECONDS.toHours(tz.getRawOffset());
        long minutes = TimeUnit.MILLISECONDS.toMinutes(tz.getRawOffset())
                - TimeUnit.HOURS.toMinutes(hours);
        // avoid -4:-30 issue
        minutes = Math.abs(minutes);

        String result = "";
        if (hours > 0) {
            result = String.format("GMT+%02d%02d", hours, minutes);
        } else {
            result = String.format("GMT%02d%02d", hours, minutes);
        }

        return result;

    }


    public static int getDaysLater(Event event) {
        Date eventDate = DateTimeUtils.getEventDate(event, 0);
        Date today = DateTimeUtils.toMidnight(Calendar.getInstance(), (event.timezone != null ? event.timezone : Event.DEFAULT_TIME_ZONE)).getTime();
        return (int) TimeUnit.MILLISECONDS.toDays(eventDate.getTime() - today.getTime());
    }

    public static int getDaysLater(EventInfoObject event) {
        Date eventDate = DateTimeUtils.getEventDate(event, 0);
        Date today = DateTimeUtils.toMidnight(Calendar.getInstance(), event.timezone).getTime();
        return (int) TimeUnit.MILLISECONDS.toDays(eventDate.getTime() - today.getTime());
    }

    public static Date getEventDate(Event event, int occurrenceNo) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(event.eventTimings.get(occurrenceNo)));
        return toMidnight(cal, (event.timezone != null ? event.timezone : Event.DEFAULT_TIME_ZONE)).getTime();
    }

    public static Date getEventDate(EventInfoObject event, int occurrenceNo) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(event.eventTimings.get(occurrenceNo)));
        return toMidnight(cal, (event.timezone != null ? event.timezone : Event.DEFAULT_TIME_ZONE)).getTime();
    }


    public static Date getCurrentDate(long timeInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(timeInMillis));
        return toMidnight(calendar, null).getTime();
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
        if (index >= event.eventTimings.size()) {
            return null;
        }

        return dateToEventTime(new Date(event.eventTimings.get(index)),
                TimeZone.getTimeZone(event.timezone != null ? event.timezone : Event.DEFAULT_TIME_ZONE), event.eventTimings.get(index));
    }

    public static
    @Nullable
    EventTime getEventTime(EventInfoObject event, int index) {
        if (index >= event.eventTimings.size()) {
            return null;
        }

        return dateToEventTime(new Date(event.eventTimings.get(index)),
                TimeZone.getTimeZone(event.timezone), event.eventTimings.get(index));
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

    public static EventTime dateToEventTime(Date date, TimeZone timeZone, long longtime) {
        synchronized (SIMPLE_DATE_FORMAT) {
            SIMPLE_DAY_FORMAT.setTimeZone(timeZone);
            SIMPLE_DATE_FORMAT.setTimeZone(timeZone);
            return new EventTime(SIMPLE_DAY_FORMAT.format(date),
                    SIMPLE_DATE_FORMAT.format(date),
                    getTimeString(date, timeZone), longtime);
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
        SimpleDateFormat FULL_DATE_TIME_MILLIS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
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

    public static String getPastTime(long milliseconds) {
        StringBuilder builder = new StringBuilder();
        long timeDifference = System.currentTimeMillis() - milliseconds;
        long days = timeDifference / 86400000;
        if (days > 0) {
            builder.append(days + ((days > 1) ? " Days " : " Day "));
            return builder.toString();
        }
        long hours = timeDifference / 3600000;
        if (hours > 0) {
            builder.append(hours + ((hours > 1) ? " Hours " : " Hour "));
            return builder.toString();
        }
        long minutes = timeDifference / 60000;
        if (minutes > 0) {
            builder.append(minutes + ((minutes > 0) ? " Mins " : " Min "));
            return builder.toString();
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

    public static String getDateFromMillisTime(long time) {
        return new SimpleDateFormat("MM/dd/yyyy").format(new Date(time));
    }

    public static long parseMovieTime(String time) {
        SimpleDateFormat DD_MM_YYYY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        try {
            date = DD_MM_YYYY_FORMAT.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

    public static long[] getWeekEndDates() {
        long[] weekendDates = new long[2];
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        weekendDates[0] = toMidnight(c, null).getTime().getTime();

        weekendDates[1] = weekendDates[0] + DateTimeUtils.MILLISECONDS_IN_A_DAY;
        return weekendDates;

    }

    private static final SimpleDateFormat HH_MM_SS_FORMAT = new SimpleDateFormat("hh:mm:ss");

    public static long getTimeInMillis(String time) {
        Date date = new Date();
        try {
            date = HH_MM_SS_FORMAT.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

    public static String getSessionDate(long time) {
        StringBuilder builder = new StringBuilder();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        builder.append(cal.get(Calendar.DAY_OF_MONTH));
        builder.append(" ");
        builder.append(new SimpleDateFormat("MMM").format(cal.getTime()));
        return builder.toString();
    }

    public static DateFormat d1 = new SimpleDateFormat("yyyy-mm-dd");
    public static DateFormat d2 = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
    public static DateFormat d3 = new SimpleDateFormat("hh:mm a");

    public static String getSessionTime(String startTime, String endTime) {
        StringBuilder builder = new StringBuilder();
        try {

            long ts = d2.parse(d1.format(new Date()) + " " + startTime).getTime();
            builder.append(d3.format(new Date(ts)));
            builder.append(" - ");
            long te = d2.parse(d1.format(new Date()) + " " + endTime).getTime();
            builder.append(d3.format(new Date(te)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static String getDateFromLongTime(long time) {
        Date date = new Date(time);
        DateFormat formatter = new SimpleDateFormat("hh:mm a, dd MMM yyyy");
        return formatter.format(date);
    }

    public static long parseZendeskTicketDate(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date date = new Date();
        try {
            date = sdf.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

}
