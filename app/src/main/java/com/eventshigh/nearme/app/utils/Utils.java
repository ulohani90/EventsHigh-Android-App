package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;

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
        builder.appendPath(webUri.getHost().replaceAll("www.", ""));
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
}
