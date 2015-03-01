package com.eventshigh.nearme.app.user;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.android.volley.Request;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.network.VolleyHelper;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Manages the user account on this device. The account information is stored using
 * SharedPreferences in {@code PREFS_FILE_NAME}.
 */
public class Account {
    // Constants used for SharedPreferences.
    private static final String PREFS_FILE_NAME = "eh_user_credentials";

    private static final String PREF_MOBILE_NO = "mobile_no";
    private static final String PREF_MOBILE_NO_VERIFIED = "mobile_no_verified";

    // The referrer for this user. this user installed the app via this referrer.
    private static final String PREF_REFERRER = "referrer";
    private static final String PREF_REFERRER_UPLOADED = "referrer_uploaded";

    // The referrer code for installation happened because of this user. This
    // user has asked his friends to install the app and passed this code.
    private static final String PREF_REFERRER_CODE = "referrer_code";
    private static final String PREF_REFERRER_CODE_UPLOADED = "referrer_code_uploaded";

    private static final String PREF_FACEBOOK_EMAIL = "facebook_email";
    private static final String PREF_FACEBOOK_EMAIL_UPLOADED = "facebook_email_uploaded";

    private static boolean disableSnackBar = false;

    // Member variables used to store the user account details in preferences.
    private final Context context;
    private final SharedPreferences accountInfo;

    public Account(Context context) {
        this.context = context;
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
            new AccountStateRegistar().execute();
            return true;
        }

        return false;
    }

    public String getUserReferrerCode() {
        String referrerCode = accountInfo.getString(PREF_REFERRER_CODE, null);
        if (referrerCode != null) {
            return referrerCode;
        }

        try {
            referrerCode = URLEncoder.encode(UUID.randomUUID().toString(), "UTF-8");
            accountInfo.edit().putString(PREF_REFERRER_CODE, referrerCode).apply();
            new AccountStateRegistar().execute();
            return referrerCode;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void recordFacebookEmail(String facebookEmail) {
        String oldFacebookMail = getFacebookEmail();
        if (oldFacebookMail == null || !oldFacebookMail.equals(facebookEmail)) {
            try {
                facebookEmail = URLEncoder.encode(facebookEmail, "UTF-8");
                accountInfo.edit()
                        .putString(PREF_FACEBOOK_EMAIL, facebookEmail)
                        .putBoolean(PREF_FACEBOOK_EMAIL_UPLOADED, false).apply();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        new AccountStateRegistar().execute();
    }

    public void getNumReferrerInstalls(
            Activity activity, final Listener<Integer> listener, ErrorListener errorListener) {
        String url = AccountStateReporter.getBaseUri(context, "getReferrerCount")
                .appendQueryParameter("referrer_id", getUserReferrerCode()).build().toString();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            new Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject, boolean isIntermediate) {
                    listener.onResponse(jsonObject.optInt("count"), isIntermediate);
                }
             }, errorListener);
        request.setTag(activity);
        VolleyHelper.addToRequestQueue(context.getApplicationContext(), request);
    }

    public @Nullable String getFacebookEmail() {
        return accountInfo.getString(PREF_FACEBOOK_EMAIL, null);
    }

    public boolean isFollowing(String tag) {
        return accountInfo.getString(getKeyForTag(tag), null) != null;
    }

    public void setIsFollowing(String tag, boolean isFollowing) {
        if (isFollowing) {
            accountInfo.edit().putString(getKeyForTag(tag), tag).apply();
        } else {
            accountInfo.edit().remove(getKeyForTag(tag)).apply();
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

    private class AccountStateRegistar extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (!accountInfo.getBoolean(PREF_REFERRER_UPLOADED, false)) {
                uploadReferrer(accountInfo.getString(PREF_REFERRER, null));
            }
            if (!accountInfo.getBoolean(PREF_REFERRER_CODE_UPLOADED, false)) {
                uploadReferrerCode(accountInfo.getString(PREF_REFERRER_CODE, null));
            }
            if (!accountInfo.getBoolean(PREF_FACEBOOK_EMAIL_UPLOADED, false)) {
                uploadFacebookEmail(accountInfo.getString(PREF_FACEBOOK_EMAIL, null));
            }

            return null;
        }

        private void uploadReferrer(@Nullable String referrer) {
            if (referrer != null) {
                AccountStateReporter.reportReferrer(context, referrer, new Runnable() {
                    @Override
                    public void run() {
                        accountInfo.edit().putBoolean(PREF_REFERRER_UPLOADED, true).apply();
                    }
                });
            }
        }

        private void uploadReferrerCode(@Nullable String referrerCode) {
            if (referrerCode != null) {
                AccountStateReporter.reportReferrerCode(context, referrerCode, new Runnable() {
                    @Override
                    public void run() {
                        accountInfo.edit().putBoolean(PREF_REFERRER_CODE_UPLOADED, true).apply();
                    }
                });
            }
        }

        private void uploadFacebookEmail(@Nullable String facebookEmail) {
            if (facebookEmail != null) {
                AccountStateReporter.reportFacebookEmail(context, facebookEmail, new Runnable() {
                    @Override
                    public void run() {
                        accountInfo.edit().putBoolean(PREF_REFERRER_CODE_UPLOADED, true).apply();
                    }
                });
            }
        }
    }
}
