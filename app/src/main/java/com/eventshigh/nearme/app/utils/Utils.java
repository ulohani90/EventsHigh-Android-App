package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.annotation.Nullable;
import android.util.ArrayMap;
import android.view.View;

import com.eventshigh.nearme.app.BuildConfig;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Helper methods for managing strings and titles.
 */
public class Utils {

    private static final int TITLE_MAX_LENGHT = 32;
    public static String shortenIfNeeded(String title) {
        return title.length() < TITLE_MAX_LENGHT ? title :
                title.substring(0, TITLE_MAX_LENGHT - 3) + "...";
    }

    public static String capitalize(String original) {
        if(original == null || original.length() == 0) {
            return original;
        }

        StringBuilder sb = new StringBuilder();
        for(String part : original.split(" ")) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append(" ");
            sb.append(part.substring(0, 1).toUpperCase());
            sb.append(part.substring(1).toLowerCase());
        }

        return sb.toString().trim();
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

        return builder.build();
    }

    public static void waitForViewVisible(final View view, final Runnable callback) {
        waitForViewVisible(view, callback, 100);
    }

    public static void waitForViewVisible(final View view, final Runnable callback, final int nTimes) {
        if (nTimes < 0) {
            return;
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                if (view.getWidth() < 10) {
                    waitForViewVisible(view, callback, nTimes - 1);
                } else {
                    try {
                        callback.run();
                    } catch (Exception e) {
                        // Ignore.
                    }
                }
            }
        }, 100);
    }

    public static String checkIfUnknown(@Nullable String string) {
        return (string == null ||
                string.isEmpty() ||
                string.equalsIgnoreCase("null") ||
                string.equalsIgnoreCase("unknown")
                ? null
                : string);
    }

    public static int dpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
    }

    public static String md5(String input) {
        try {
            MessageDigest mdEnc = MessageDigest.getInstance("MD5");
            mdEnc.update(input.getBytes(), 0, input.length());
            return new BigInteger(1, mdEnc.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw  new RuntimeException(e);
        }
    }


    private static final Set<String> DEBUG_ANDROID_ID = new HashSet<>();
    static {
        DEBUG_ANDROID_ID.add("5151a4342221f321");  // Parag
        DEBUG_ANDROID_ID.add("5f0f51994cb15c85");  // Arvind
        DEBUG_ANDROID_ID.add("66440e253daf9b3d");  // Nikesh
        DEBUG_ANDROID_ID.add("56a538060a00eaa6");  // Samsung-duos
        DEBUG_ANDROID_ID.add("8fa00ccd84f6351b");  // eh
        DEBUG_ANDROID_ID.add("b84b8ede41e9dc36");  // Umesh
        DEBUG_ANDROID_ID.add("d5af6ff15811e26d");  // Simran
    }

    public static boolean isDebug(Context context) {
        return BuildConfig.DEBUG || DEBUG_ANDROID_ID.contains(getAndroidId(context));
    }

    private static Pattern phoneNoPattern = Pattern.compile("[^\\d\\+]");
    public static String simplifyPhoneNo(String phoneNo) {
        return phoneNoPattern.matcher(phoneNo).replaceAll("");
    }

    public static <K,V> Map<K,V> getMap() {
        if (android.os.Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            return new ArrayMap<>();
        } else {
            return new HashMap<>();
        }
    }

    public static int getRandomNumber(int low, int high){
        Random r = new Random();
        return r.nextInt(high-low) + low;
    }
}
