package com.eventshigh.nearme.app.utils;

import android.net.Uri;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper methods for managing strings and titles.
 */
public class Utils {

    private static final int TITLE_MAX_LENGHT = 32;
    public static String shortenIfNeeded(String title) {
        return title.length() < TITLE_MAX_LENGHT ? title :
                title.substring(0, TITLE_MAX_LENGHT - 3) + "...";
    }

    public static String capitalize(String original){
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

    public static <T> T[] mergeArray(T[] first, T[] second) {
        List<T> both = new ArrayList<>(first.length + second.length);
        Collections.addAll(both, first);
        Collections.addAll(both, second);
        return both.toArray(first);
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

    public static Pair<Integer, Integer> findDimensions(View view, DisplayMetrics metrics) {
        int width = view.getWidth();
        int height = view.getHeight();

        if (width <= 0 || height <= 0) {
            int parentWidth = getDimen(((ViewGroup) view.getParent()).getWidth(), metrics.widthPixels);
            int parentHeight = getDimen(((ViewGroup) view.getParent()).getHeight(), metrics.heightPixels);
            width = getDimen(width, parentWidth);
            height = getDimen(height, parentHeight);

            view.measure(parentWidth, parentHeight);
            width = getDimen(width, view.getMeasuredWidth());
            height = getDimen(height, view.getMeasuredHeight());
        }

        return Pair.create(width, height);
    }

    private static int getDimen(int dimen1, int dimen2) {
        return  dimen1 <= 0 ? dimen2 :
                (dimen2 <= 0 ? dimen1 : Math.min(dimen1, dimen2));
    }
}
