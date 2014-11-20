package com.eventshigh.nearme.app;

import android.location.Location;

import com.eventshigh.nearme.app.data.Event;
import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

public class Utils {

    private Utils() {
        // No public constructor for helper class.
    }

    public static LatLng locationToLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public static Date getDate(int dayItemNo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, dayItemNo);
        return cal.getTime();
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static String getDateString(Date date) {
        return DATE_FORMAT.format(date);
    }

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static Date mergeDateTime(String date, String time) throws ParseException {
        if (date == null || time == null
                || date.isEmpty() || time.isEmpty()
                || date.equalsIgnoreCase("null") || time.equalsIgnoreCase("null")) {
            return null;
        }

        if (time.split(":").length == 2) {
            time = time + ":00";
        }

        return DATE_TIME_FORMAT.parse(date.split(":")[0] + " " + time);
    }

    private static final int TITLE_MAX_LENGHT = 32;
    public static String shortenIfNeeded(String title) {
        return title.length() < TITLE_MAX_LENGHT ? title :
                title.substring(0, TITLE_MAX_LENGHT - 3) + "...";
    }

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a");
    private static final Pattern ZEROS = Pattern.compile(":00");
    private static final Pattern LEADING_ZEROS = Pattern.compile("0\\d");
    public static String getEventTime(Event event) {
        String time = TIME_FORMAT.format(event.startTime) +
                (event.endTime == null ? "" : " - " + TIME_FORMAT.format(event.endTime));
        return  LEADING_ZEROS.matcher(ZEROS.matcher(time).replaceAll("")).replaceAll("\\1");
    }
}
