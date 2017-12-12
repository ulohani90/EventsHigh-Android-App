package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.View;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
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

    public static final String ADWORDS_CONVERSION_ID = "959501249";

    public static final String YOUTUBE_API_KEY = "AIzaSyACL1eHX6pkvIAsFk1VqN-TDAgzS14Pwek";

    private static final int TITLE_MAX_LENGHT = 32;

    public static String shortenIfNeeded(String title) {
        try {
            return title.length() < TITLE_MAX_LENGHT ? title :
                    title.substring(0, TITLE_MAX_LENGHT - 3) + "...";
        } catch (Exception e) {
            return null;
        }
    }

    public static String capitalize(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }

        StringBuilder sb = new StringBuilder();
        for (String part : original.split(" ")) {
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
                        Crashlytics.getInstance().core.logException(e);
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

    public static boolean checkIfStringEmpty(@Nullable String string) {
        return (string == null ||
                string.isEmpty() ||
                string.equalsIgnoreCase("null") ||
                string.equalsIgnoreCase("unknown")
                ? true
                : false);
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
            throw new RuntimeException(e);
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

    public static <K, V> Map<K, V> getMap() {
        if (android.os.Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            return new ArrayMap<>();
        } else {
            return new HashMap<>();
        }
    }

    public static int getRandomNumber(int low, int high) {
        Random r = new Random();
        return r.nextInt(high - low) + low;
    }

    public static boolean isValidEmail(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public static boolean isValidPhone(String phoneNo) {
        String Regex = "[^\\d] ";
        String PhoneDigits = phoneNo.replaceAll(Regex, "");
        if (PhoneDigits.length() != 10) {
            return false;
        } else {
            return true;
        }
    }

    public static Double roundToTwoDecimalPlaces(Double value) {
        try {
            DecimalFormat format = new DecimalFormat("#.#");
            return Double.valueOf(format.format(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static int getDummyImageResource() {
        Random random = new Random();
        int num = random.nextInt(20 - 1 + 1) + 1;

        switch (num) {
            case 1:
                return R.drawable.ic_dummy_1;
            case 2:
                return R.drawable.ic_dummy_2;
            case 3:
                return R.drawable.ic_dummy_3;
            case 4:
                return R.drawable.ic_dummy_4;
            case 5:
                return R.drawable.ic_dummy_5;
            case 6:
                return R.drawable.ic_dummy_6;
            case 7:
                return R.drawable.ic_dummy_7;
            case 8:
                return R.drawable.ic_dummy_8;
            case 9:
                return R.drawable.ic_dummy_9;
            case 10:
                return R.drawable.ic_dummy_10;
            case 11:
                return R.drawable.ic_dummy_11;
            case 12:
                return R.drawable.ic_dummy_12;
            case 13:
                return R.drawable.ic_dummy_13;
            case 14:
                return R.drawable.ic_dummy_14;
            case 15:
                return R.drawable.ic_dummy_15;
            case 16:
                return R.drawable.ic_dummy_16;
            case 17:
                return R.drawable.ic_dummy_17;
            case 18:
                return R.drawable.ic_dummy_18;
            case 19:
                return R.drawable.ic_dummy_19;
            case 20:
                return R.drawable.ic_dummy_20;
            default:
                return R.drawable.ic_dummy_5;


        }
    }

    public static String getUnderscoreString(String name) {
        name = name.replace(" ", "_").replace("/", "_").replace("-", "_");
        return name.toLowerCase();
    }

    public static String changedHeaderHtml(String htmlText) {

        String head = "<head><meta name=\"viewport\" content=\"width=device-width,user-scalable=yes,text/html,charset=utf-8\" /></head>";

        String closedTag = "</body></html>";
        String changeFontHtml = head + htmlText + closedTag;
        return changeFontHtml;
    }
}
