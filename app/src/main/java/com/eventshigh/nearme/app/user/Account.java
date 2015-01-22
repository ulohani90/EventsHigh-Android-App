package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * Manages the user account on this device. The account information is stored using
 * SharedPreferences in {@code PREFS_FILE_NAME}.
 */
public class Account {
    // Constants used for SharedPreferences.
    private static final String PREFS_FILE_NAME = "eh_user_credentials";

    private static final String PREF_PHONE_NO = "phone_no";
    private static final String PREF_NUM_LOGIN_ATTEMPTS = "num_login_attempts";
    private static final String PREF_ASK_LOGIN = "ask_login";

    // The referrer for this user. this user installed the app via this referrer.
    private static final String PREF_REFERRER = "referrer";
    private static final String PREF_REFERRER_UPLOADED = "referrer_uploaded";

    // The referrer code for installation happened because of this user. This
    // user has asked his friends to install the app and passed this code.
    private static final String PREF_REFERRER_CODE = "referrer_code";
    private static final String PREF_REFERRER_CODE_UPLOADED = "referrer_code_uploaded";

    private static final String PREF_FACEBOOK_EMAIL = "facebook_email";
    private static final String PREF_FACEBOOK_EMAIL_UPLOADED = "facebook_email_uploaded";

    // Constant used to skip the login screen if there are too many failed login attempts
    private static final int NUM_MAX_LOGIN_ATTEMPT = 3;

    // Member variables used to store the user account details in preferences.
    private final Context context;
    private final SharedPreferences accountInfo;

    public Account(Context context) {
        this.context = context;
        accountInfo = context.getSharedPreferences(PREFS_FILE_NAME, 0);

        // Check if we need to upload the data.
        new AccountStateRegistar().execute();
    }

    public boolean shouldAskForLogin() {
        return accountInfo.getBoolean(PREF_ASK_LOGIN, true);
    }

    public boolean recordLoginFailure() {
        boolean tooManyFailures = false;
        int numFailedLogin = accountInfo.getInt(PREF_NUM_LOGIN_ATTEMPTS, 0) + 1;
        SharedPreferences.Editor editor = accountInfo.edit();
        editor.putInt(PREF_NUM_LOGIN_ATTEMPTS, numFailedLogin);
        if (numFailedLogin >= NUM_MAX_LOGIN_ATTEMPT) {
            tooManyFailures = true;
            editor.putBoolean(PREF_ASK_LOGIN, false);
        }
        editor.apply();

        return tooManyFailures;
    }

    public void recordLoginSuccess(String phoneNumber) {
        SharedPreferences.Editor editor = accountInfo.edit();
        editor.putString(PREF_PHONE_NO, phoneNumber);
        editor.putBoolean(PREF_ASK_LOGIN, false);
        editor.apply();
    }

    public void recordSkipLogin() {
        accountInfo.edit().putBoolean(PREF_ASK_LOGIN, false).apply();
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
        try {
            facebookEmail = URLEncoder.encode(facebookEmail, "UTF-8");
            accountInfo.edit()
                    .putString(PREF_FACEBOOK_EMAIL, facebookEmail)
                    .putBoolean(PREF_FACEBOOK_EMAIL_UPLOADED, false).apply();
            new AccountStateRegistar().execute();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public @Nullable String getFacebookEmail() {
        return accountInfo.getString(PREF_FACEBOOK_EMAIL, null);
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
                if (AccountStateReporter.reportReferrer(context, referrer)) {
                    accountInfo.edit().putBoolean(PREF_REFERRER_UPLOADED, true).apply();
                }
            }
        }

        private void uploadReferrerCode(@Nullable String referrerCode) {
            if (referrerCode != null) {
                if (AccountStateReporter.reportReferrerCode(context, referrerCode)) {
                    accountInfo.edit().putBoolean(PREF_REFERRER_CODE_UPLOADED, true).apply();
                }
            }
        }

        private void uploadFacebookEmail(@Nullable String facebookEmail) {
            if (facebookEmail != null) {
                if (AccountStateReporter.reportFacebookEmail(context, facebookEmail)) {
                    accountInfo.edit().putBoolean(PREF_REFERRER_CODE_UPLOADED, true).apply();
                }
            }
        }
    }
}
