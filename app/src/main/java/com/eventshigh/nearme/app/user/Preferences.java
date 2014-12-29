package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * User Preferences.
 */
public class Preferences {
    private static final String PREF_DEFAULT_ACTIVITY_MAPS = "eh_pref_default_activity_MAPS";
    private static final String PREF_NUM_ACTIVITIES = "eh_pref_num_activities";

    private final SharedPreferences sharedPreferences;

    public Preferences(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isMapsViewDefault() {
        return sharedPreferences.getBoolean(PREF_DEFAULT_ACTIVITY_MAPS, false);
    }

    public int reportActivityStart() {
        int numActions = sharedPreferences.getInt(PREF_NUM_ACTIVITIES, 0) + 1;
        sharedPreferences.edit().putInt(PREF_NUM_ACTIVITIES, numActions).apply();
        return numActions;
    }

    public int getNumActivities() {
        return sharedPreferences.getInt(PREF_NUM_ACTIVITIES, 0);
    }
}
