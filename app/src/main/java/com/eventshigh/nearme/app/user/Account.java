package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;

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

    private static final String PREF_REFERRER = "referrer";

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
        editor.putLong(PREF_USER_ID, session.getId());
        editor.putString(PREF_PHONE_NO, phoneNumber);
        editor.putBoolean(PREF_ASK_LOGIN, false);
        editor.apply();
    }
    **/

    public void recordSkipLogin() {
        accountInfo.edit().putBoolean(PREF_ASK_LOGIN, false).apply();
    }

    public boolean recordReferrer(String referrer) {
        if (!accountInfo.contains(PREF_REFERRER)) {
            accountInfo.edit().putString(PREF_REFERRER, referrer).apply();
            return true;
        }

        return false;
    }
}
