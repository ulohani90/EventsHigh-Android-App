package com.eventshigh.nearme.app.settings;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

/**
 * User Preferences an simple wrapper across
 * {@link android.preference.PreferenceManager#getDefaultSharedPreferences(android.content.Context)}
 */
public class Preferences implements OnSharedPreferenceChangeListener {
    public static final String PREF_DEFAULT_ACTIVITY = "default_activity";
    public static final String PREF_NOTIFY_WEEKEND = "notifications_weekend";
    public static final String PREF_NOTIFY_NEARBY = "notifications_nearby";
    public static final String PREF_SHOW_ONBOARDING = "show_onboarding";

    private final Context context;
    private final SharedPreferences sharedPreferences;

    private Preferences(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private static Preferences instance;
    public static synchronized Preferences getInstance(Context context) {
        if (instance == null) {
            instance = new Preferences(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isMapsViewDefault() {
        return "maps".equals(sharedPreferences.getString(PREF_DEFAULT_ACTIVITY, ""));
    }

    public boolean shouldNotifyWeekend() {
        return sharedPreferences.getBoolean(PREF_NOTIFY_WEEKEND, true);
    }

    public boolean shouldNotifyNearBy() {
        return sharedPreferences.getBoolean(PREF_NOTIFY_NEARBY, true);
    }

    public void setShowOnboarding(boolean shouldShowOnboarding) {
        sharedPreferences.edit().putBoolean(PREF_SHOW_ONBOARDING, shouldShowOnboarding).apply();
    }

    public boolean shouldShowOnBoarding() {
        return sharedPreferences.getBoolean(PREF_SHOW_ONBOARDING, true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        new BackupManager(context).dataChanged();
    }

}
