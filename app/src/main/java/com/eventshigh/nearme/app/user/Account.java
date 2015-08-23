package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.Signer;
import com.eventshigh.nearme.app.user.UserActionHelper.FollowingAction;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * Manages the user account on this device. The account information is stored using
 * SharedPreferences in {@code PREFS_FILE_NAME}.
 */
public class Account {
    public static class UserInfo {
        @Nullable public final String name;
        @Nullable public final String phoneNo;
        public final Boolean isVerified;

        public UserInfo(@Nullable String name, @Nullable String phoneNo, Boolean isVerified) {
            this.name = name;
            this.phoneNo = phoneNo;
            this.isVerified = isVerified;
        }
    }

    // Constants used for SharedPreferences.
    private static final String PREFS_FILE_NAME = "eh_user_credentials";

    // Mobile no of the user.
    private static final String PREF_NAME = "name";
    private static final String PREF_MOBILE_NO = "mobile_no";
    private static final String PREF_MOBILE_NO_VERIFIED = "mobile_no_verified";

    // The referrer for this user. this user installed the app via this referrer.
    private static final String PREF_REFERRER = "referrer";
    private static final String PREF_REFERRER_UPLOADED = "referrer_uploaded";

    // The app download link for the user. Each user has unique link so that we can
    // track the number of installs.
    private static final String PREF_SHARE_APP_LINK = "app_download_link";

    // The prefix to the shared prefs key used to save follow tags for this user
    private static final String PREF_FOLLOW_KEY_PREFIX = "follow_";

    private static final Object syncLock = false;
    private static long lastSyncTimestamp = 0;
    private static boolean disableSnackBar = false;

    // shared and static accountInfo which usages shared preference to store records.
    private static SharedPreferences accountInfo;
    private static synchronized void setAccountInfo(Context context) {
        if (accountInfo == null) {
            accountInfo = context.getSharedPreferences(PREFS_FILE_NAME, 0);
        }
    }

    // Member variables used to store the user account details in preferences.
    private final Context context;

    public Account(Context context) {
        this.context = context.getApplicationContext();

        setAccountInfo(this.context);

        // Check if we need to upload the data.
        syncIfNeeded();
    }

    public UserInfo getUserInfo() {
        return new UserInfo(
                accountInfo.getString(PREF_NAME, null),
                accountInfo.getString(PREF_MOBILE_NO, null),
                accountInfo.getBoolean(PREF_MOBILE_NO_VERIFIED, false));
    }

    public void recordPhoneNumber(String name, String phoneNumber) {
        SharedPreferences.Editor editor = accountInfo.edit();
        editor.putString(PREF_NAME, name);
        editor.putString(PREF_MOBILE_NO, phoneNumber);
        editor.remove(PREF_MOBILE_NO_VERIFIED);
        editor.apply();
    }

    public void recordVerifiedPhoneNumber() {
        SharedPreferences.Editor editor = accountInfo.edit();
        editor.putBoolean(PREF_MOBILE_NO_VERIFIED, true);
        editor.apply();
    }

    public void removeUserInfo() {
        SharedPreferences.Editor editor = accountInfo.edit();
        editor.remove(PREF_NAME);
        editor.remove(PREF_MOBILE_NO);
        editor.remove(PREF_MOBILE_NO_VERIFIED);
        editor.apply();
    }

    public static void disablePhoneVerifySnackbar() {
        disableSnackBar = true;
    }

    public static boolean isPhoneVerifyPending(Context context) {
        if (disableSnackBar) {
            return false;
        }

        Account account = new Account(context);
        UserInfo userInfo = account.getUserInfo();
        return userInfo.phoneNo != null && !userInfo.isVerified;
    }

    public boolean recordReferrer(String referrer) {
        if (!accountInfo.contains(PREF_REFERRER)) {
            accountInfo.edit().putString(PREF_REFERRER, referrer).apply();

            synchronized (syncLock) {
                lastSyncTimestamp = 0;
                syncIfNeeded();
            }
            return true;
        }

        return false;
    }

    public String getAppDownloadLink() {
        String ret = accountInfo.getString(PREF_SHARE_APP_LINK, null);
        if (ret == null) {
            ret = Uri.parse("https://play.google.com/store/apps/details").buildUpon()
                        .appendQueryParameter("id", context.getPackageName())
                        .appendQueryParameter("referrer", Utils.getAndroidId(context))
                        .build().toString();
        }
        return ret;
    }

    public boolean isFollowing(String tag) {
        return accountInfo.getString(getKeyForTag(tag), null) != null;
    }

    public void setIsFollowing(String tag, boolean isFollowing) {
        if (isFollowing) {
            accountInfo.edit().putString(getKeyForTag(tag), tag).apply();
            new UserActionHelper(context).recordAction(FollowingAction.FOLLOW, tag);
            GcmRegistration.getInstance(context).subscribeToTopic(tag);
        } else {
            accountInfo.edit().remove(getKeyForTag(tag)).apply();
            new UserActionHelper(context).recordAction(FollowingAction.UN_FOLLOW, tag);
            GcmRegistration.getInstance(context).unSubscribeToTopic(tag);
        }
    }

    public List<String> getFollowingInterests() {
        List<String> interests = new ArrayList<>();
        for (Entry<String, ?> entry : accountInfo.getAll().entrySet()) {
            if (entry.getKey().startsWith("follow_")) {
                interests.add(entry.getValue().toString());
            }
        }
        return interests;
    }

    private static String getKeyForTag(String tag) {
        return PREF_FOLLOW_KEY_PREFIX + EventCategory.toCategoryParsableString(tag);
    }

    private void syncIfNeeded() {
        if (lastSyncTimestamp < System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)) {
            new Thread(new AccountStateRegistar()).start();
        }
    }

    private class AccountStateRegistar implements Runnable {
        @Override
        public void run() {
            synchronized (syncLock) {
                if (lastSyncTimestamp > System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)) {
                    return;
                }
                lastSyncTimestamp = System.currentTimeMillis();
            }

            String referrer = accountInfo.getString(PREF_REFERRER, null);
            String appLink = accountInfo.getString(PREF_SHARE_APP_LINK, null);
            if (appLink != null && (referrer == null || accountInfo.getBoolean(PREF_REFERRER_UPLOADED, false))) {
                return;
            }

            if (referrer != null && !accountInfo.getBoolean(PREF_REFERRER_UPLOADED, false)) {
                AccountStateReporter.reportReferrer(context, referrer, new Runnable() {
                    @Override
                    public void run() {
                        accountInfo.edit().putBoolean(PREF_REFERRER_UPLOADED, true).apply();
                    }
                });
            }

            if (referrer != null || GcmRegistration.getInstance(context).getLastCity() != null) {
                try {
                    Uri uri = AccountStateReporter.getBaseUri(context, "getReferrerLink").build();
                    String url = Signer.sign(uri).toString();
                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                            appLinkListener, errorListener);
                    VolleyHelper.addToRequestQueue(context, request);
                } catch (IOException | GeneralSecurityException e) {
                    Crashlytics.getInstance().core.logException(e);
                    Log.w(Account.class.getSimpleName(), "failed to get shortlink", e);
                }
            }
        }

        private Listener<JSONObject> appLinkListener = new Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject, boolean isIntermediate) {
                String link = jsonObject.optString("link");
                if (link != null && !link.isEmpty()) {
                    accountInfo.edit().putString(PREF_SHARE_APP_LINK, link).apply();
                }
            }
        };

        private ErrorListener errorListener = new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Crashlytics.getInstance().core.logException(volleyError.getCause());
            }
        };
    }
}
