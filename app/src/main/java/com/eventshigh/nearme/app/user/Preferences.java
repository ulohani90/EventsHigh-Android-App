package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * User Preferences.
 */
public class Preferences {
    private static final String PREF_DEFAULT_ACTIVITY_MAPS = "eh_pref_default_activity_MAPS";
    private static final String PREF_FIRST_ACTION = "eh_pref_first_action";

    private final SharedPreferences sharedPreferences;

    public Preferences(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isMapsViewDefault() {
        return sharedPreferences.getBoolean(PREF_DEFAULT_ACTIVITY_MAPS, false);
    }

    public boolean isFirstActivity() {
        boolean isFirst = sharedPreferences.getBoolean(PREF_FIRST_ACTION, true);
        if (isFirst) {
            sharedPreferences.edit().putBoolean(PREF_FIRST_ACTION, false).apply();
        }
        return isFirst;
    }
}
