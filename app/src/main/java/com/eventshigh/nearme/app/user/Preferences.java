package com.eventshigh.nearme.app.user;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

/**
 * User Preferences an simple wrapper across
 * {@link android.preference.PreferenceManager#getDefaultSharedPreferences(android.content.Context)}
 */
public class Preferences implements OnSharedPreferenceChangeListener {
    public static final String PREF_NOTIFY_FAVOURITED = "notifications_favourited";
    public static final String PREF_NOTIFY_WEEKLY = "notifications_weekly";
    public static final String PREF_NOTIFY_EH = "notifications_eh";
    public static final String PREF_SHOW_ONBOARDING = "show_onboarding";
    public static final String PREF_OFFER_TO_SHOW = "offer_to_show";
    public static final String PREF_NUM_TIMES_MY_EVENTS_CLUE_SHOWN = "my_events_shown_count";

    private final Context context;
    private final SharedPreferences sharedPreferences;

    private Preferences(Context context) {
        this.context = context.getApplicationContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private static Preferences instance;
    public static synchronized Preferences getInstance(Context context) {
        if (instance == null) {
            instance = new Preferences(context);
        }
        return instance;
    }

    public boolean shouldNotifyFavourited() {
        return sharedPreferences.getBoolean(PREF_NOTIFY_FAVOURITED, true);
    }

    public boolean shouldNotifyWeekly() {
        return sharedPreferences.getBoolean(PREF_NOTIFY_WEEKLY, true);
    }

    public boolean shouldNotifyEHRecommendation() {
        return sharedPreferences.getBoolean(PREF_NOTIFY_EH, true);
    }

    public void setShowOnboarding(boolean shouldShowOnboarding) {
        sharedPreferences.edit().putBoolean(PREF_SHOW_ONBOARDING, shouldShowOnboarding).apply();
    }

    public boolean shouldShowOnBoarding() {
        return sharedPreferences.getBoolean(PREF_SHOW_ONBOARDING, true);
    }

    public void setOfferURI(String offerURI) {
        sharedPreferences.edit().putString(PREF_OFFER_TO_SHOW, offerURI).apply();
    }

    public void deleteOfferURI() {
        sharedPreferences.edit().remove(PREF_OFFER_TO_SHOW).apply();
    }

    public @Nullable String getOfferURI() {
        return sharedPreferences.getString(PREF_OFFER_TO_SHOW, null);
    }

    public int getNumTimesMyEventsClueShown() {
        return sharedPreferences.getInt(PREF_NUM_TIMES_MY_EVENTS_CLUE_SHOWN, 0);
    }

    public void incrementNumTimesMyEventsClueShown() {
        sharedPreferences.edit().putInt(PREF_NUM_TIMES_MY_EVENTS_CLUE_SHOWN,
                getNumTimesMyEventsClueShown() + 1).apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        new BackupManager(context).dataChanged();
    }

}
