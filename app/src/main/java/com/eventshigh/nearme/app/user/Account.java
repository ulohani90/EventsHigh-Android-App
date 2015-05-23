package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.android.volley.Request;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.UserActionDbHelper;
import com.eventshigh.nearme.app.data.UserActionDbHelper.FollowingAction;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Manages the user account on this device. The account information is stored using
 * SharedPreferences in {@code PREFS_FILE_NAME}.
 */
public class Account {
    // Constants used for SharedPreferences.
    private static final String PREFS_FILE_NAME = "eh_user_credentials";

    // Mobile no of the user.
    private static final String PREF_MOBILE_NO = "mobile_no";
    private static final String PREF_MOBILE_NO_VERIFIED = "mobile_no_verified";

    // The referrer for this user. this user installed the app via this referrer.
    private static final String PREF_REFERRER = "referrer";
    private static final String PREF_REFERRER_UPLOADED = "referrer_uploaded";

    // The app download link for the user. Each user has unique link so that we can
    // track the number of installs.
    private static final String PREF_SHARE_APP_LINK = "app_download_link";

    private static boolean disableSnackBar = false;

    // Member variables used to store the user account details in preferences.
    private final Context context;
    private final SharedPreferences accountInfo;

    public Account(Context context) {
        this.context = context.getApplicationContext();
        accountInfo = context.getSharedPreferences(PREFS_FILE_NAME, 0);

        // Check if we need to upload the data.
        new AccountStateRegistar().execute();
    }

    public Pair<String, Boolean> getPhoneNumber() {
        return Pair.create(accountInfo.getString(PREF_MOBILE_NO, null),
                accountInfo.getBoolean(PREF_MOBILE_NO_VERIFIED, false));
    }

    public void recordPhoneNumber(String phoneNumber) {
        SharedPreferences.Editor editor = accountInfo.edit();
        editor.putString(PREF_MOBILE_NO, phoneNumber);
        editor.remove(PREF_MOBILE_NO_VERIFIED);
        editor.apply();
    }

    public void recordVerifiedPhoneNumber() {
        SharedPreferences.Editor editor = accountInfo.edit();
        editor.putBoolean(PREF_MOBILE_NO_VERIFIED, true);
        editor.apply();
    }

    public void removePhoneNumber() {
        SharedPreferences.Editor editor = accountInfo.edit();
        editor.remove(PREF_MOBILE_NO);
        editor.remove(PREF_MOBILE_NO_VERIFIED);
        editor.apply();
    }

    public static void disablePhoneVerifySnackbar() {
        Account.disableSnackBar = true;
    }

    public static boolean isPhoneVerifyPending(Context context) {
        if (disableSnackBar) {
            return false;
        }
        Account account = new Account(context);
        Pair<String, Boolean> accountPhoneStatus = account.getPhoneNumber();
        return accountPhoneStatus.first != null && !accountPhoneStatus.second;
    }

    public boolean recordReferrer(String referrer) {
        if (!accountInfo.contains(PREF_REFERRER)) {
            accountInfo.edit().putString(PREF_REFERRER, referrer).apply();
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
            UserActionDbHelper.getInstance(context).recordAction(FollowingAction.FOLLOW, tag);

        } else {
            accountInfo.edit().remove(getKeyForTag(tag)).apply();
            UserActionDbHelper.getInstance(context).recordAction(FollowingAction.UN_FOLLOW, tag);
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
        return "follow_" + EventCategory.toCategoryParsableString(tag);
    }

    private static final Object AccountStateRegistarLock = new Object();
    private static boolean inProgress = false;

    private class AccountStateRegistar extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            String referrer = accountInfo.getString(PREF_REFERRER, null);
            if (referrer != null && !accountInfo.getBoolean(PREF_REFERRER_UPLOADED, false)) {
                uploadReferrer(referrer);
            }

            synchronized (AccountStateRegistarLock) {
                if (!inProgress &&
                    accountInfo.getString(PREF_SHARE_APP_LINK, null) == null &&
                    GcmRegistration.getInstance(context).getLastCity() != null) {
                    try {
                        Uri uri = AccountStateReporter.getBaseUri(context, "getReferrerLink").build();
                        String url = Signer.sign(uri).toString();
                        inProgress = true;
                        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                                new Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject jsonObject, boolean isIntermediate) {
                                        String link = jsonObject.optString("link");
                                        if (link != null && !link.isEmpty()) {
                                            accountInfo.edit().putString(PREF_SHARE_APP_LINK, link).apply();
                                        }
                                        synchronized (AccountStateRegistarLock) {
                                            inProgress = false;
                                        }
                                    }
                                },
                                new ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError volleyError) {
                                        synchronized (AccountStateRegistarLock) {
                                            inProgress = false;
                                        }
                                    }
                                }
                        );
                        VolleyHelper.addToRequestQueue(context, request);
                    } catch (IOException | GeneralSecurityException e) {
                        Log.w(Account.class.getSimpleName(), "failed to get shortlink", e);
                    }
                }
            }
            return null;
        }

        private void uploadReferrer(String referrer) {
            AccountStateReporter.reportReferrer(context, referrer, new Runnable() {
                @Override
                public void run() {
                    accountInfo.edit().putBoolean(PREF_REFERRER_UPLOADED, true).apply();
                }
            });
        }
    }
}
