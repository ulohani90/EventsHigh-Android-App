package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.eventshigh.nearme.app.data.Event;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Utility class. All Misc helper methods are here.
 */
public class Utils {

    private Utils() {
        // No public constructor for helper class.
    }


    /*******************************************************
     Helper methods for managing location and latlng
     *******************************************************/
    public static LatLng locationToLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public static float distanceInMeters(LatLng loc1, LatLng loc2) {
        float[] distance = new float[1];
        Location.distanceBetween(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude, distance);
        return distance[0];
    }

    public static float getDistanceSQ(Point p1, Point p2) {
        return (p1.x - p2.x) * (p1.x - p2.x) +  (p1.y - p2.y) * (p1.y - p2.y);
    }


    /*******************************************************
     Helper methods for managing date and time.
     *******************************************************/
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
    public static @Nullable Date mergeDateTime(String date, String time, String timeZone) throws ParseException {
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

    /*******************************************************
     Helper methods for managing strings and titles.
     *******************************************************/
    private static final int TITLE_MAX_LENGHT = 32;
    public static String shortenIfNeeded(String title) {
        return title.length() < TITLE_MAX_LENGHT ? title :
                title.substring(0, TITLE_MAX_LENGHT - 3) + "...";
    }

    public static String capitalize(String original){
        if(original == null || original.length() == 0) {
            return original;
        }

        return original.substring(0, 1).toUpperCase() + original.substring(1).toLowerCase();
    }

    public static <T> T[] mergeArray(T[] first, T[] second) {
        List<T> both = new ArrayList<>(first.length + second.length);
        Collections.addAll(both, first);
        Collections.addAll(both, second);
        return both.toArray(first);
    }


    /*******************************************************
     Helper methods for reading stream or asserts file
     *******************************************************/
    public static String[] readAssetFile(Context context, String filename) throws IOException {
        InputStream is = context.getAssets().open(filename);
        try {
            return readStream(is);
        } finally {
            is.close();
        }
    }

    public static String[] readStream(InputStream is) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return lines.toArray(new String[lines.size()]);
    }

    public static JSONObject fetchJSON(String url) throws IOException, JSONException {
        HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();
        try {
            StringBuilder jsonBuffer = new StringBuilder();
            for (String jsonStr : Utils.readStream(urlConnection.getInputStream())) {
                jsonBuffer.append(jsonStr);
            }

            return new JSONObject(jsonBuffer.toString());
        } finally {
            urlConnection.disconnect();
        }
    }

    public static Uri getAppUri(Uri webUri) {
        Uri.Builder builder = Uri.parse("android-app://com.eventshigh.nearme.app/").buildUpon();
        builder.appendPath(webUri.getScheme());
        builder.appendPath(webUri.getHost());
        for (String pathSegment : webUri.getPathSegments()) {
            builder.appendPath(pathSegment);
        }
        if (webUri.getQuery() != null) {
            builder.encodedQuery(webUri.getEncodedQuery());
        }

        Uri androidUri = builder.build();
        Log.w("TEXT", "web= '" + webUri.toString() + "', app='"+ androidUri.toString() + "'");
        return  androidUri;
    }
}
