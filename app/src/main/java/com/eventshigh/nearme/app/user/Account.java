package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;

import com.eventshigh.nearme.app.user.AccountStateReporter.ReferrerIdReporter;
import com.eventshigh.nearme.app.user.AccountStateReporter.ReferrerReporter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * Manages the user account on this device. The account information is stored in
 * SharedPreferences.
 */
public class Account {
    // Constants used for SharedPreferences.
    private static final String PREFS_FILE_NAME = "eh_user_credentials";

    private static final String PREF_DIGITS_USER_ID = "digits_user_id";
    private static final String PREF_PHONE_NO = "phone_no";
    private static final String PREF_NUM_LOGIN_ATTEMPTS = "num_login_attempts";
    private static final String PREF_ASK_LOGIN = "ask_login";

    // The referrer for this user. this user installed the app via this referrer.
    private static final String PREF_REFERRER = "referrer";

    // The referrer code for installation happened because of this user. This
    // user has asked his friends to install the app and passed this code.
    private static final String PREF_USER_REFERRER_CODE = "user_referrer_code";

    // Constant used to skip the login screen if there are too many failed login attempts
    private static final int NUM_MAX_LOGIN_ATTEMPT = 3;

    // Member variables used to store the user account details in preferences.
    private SharedPreferences accountInfo;

    public Account(Context context) {
        accountInfo = context.getSharedPreferences(PREFS_FILE_NAME, 0);
    }

    public boolean shouldAskForLogin() {
        return accountInfo.getBoolean(PREF_ASK_LOGIN, true);
    }

    public boolean recordFailure() {
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

    /**
    public void recordSuccess(DigitsSession session, String phoneNumber) {
        SharedPreferences.Editor editor = accountInfo.edit();
        editor.putLong(PREF_DIGITS_USER_ID, session.getId());
        editor.putString(PREF_PHONE_NO, phoneNumber);
        editor.putBoolean(PREF_ASK_LOGIN, false);
        editor.apply();
    }
    **/

    public void recordSkipLogin() {
        accountInfo.edit().putBoolean(PREF_ASK_LOGIN, false).apply();
    }

    public boolean recordReferrer(Context context, String referrer) {
        if (!accountInfo.contains(PREF_REFERRER)) {
            accountInfo.edit().putString(PREF_REFERRER, referrer).apply();
            new ReferrerReporter(context).execute(referrer);
            return true;
        }

        return false;
    }

    public String getUserReferrerCode(Context context) {
        String referrerCode = accountInfo.getString(PREF_USER_REFERRER_CODE, null);
        if (referrerCode != null) {
            return referrerCode;
        }

        try {
            referrerCode = URLEncoder.encode(UUID.randomUUID().toString(), "UTF-8");
            accountInfo.edit().putString(PREF_USER_REFERRER_CODE, referrerCode).apply();
            new ReferrerIdReporter(context).execute(referrerCode);
            return referrerCode;
        } catch (UnsupportedEncodingException e) {
            // Ignore.
            return null;
        }
    }
}
