package com.eventshigh.nearme.app.user;

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
    public static final String PREF_SHOW_ONBOARDING = "show_onboarding";
    public static final String PREF_UPLOAD_CONTACTS = "upload_contacts";
    public static final String PREF_LAST_TIME_ASKED_CONTACTS = "last_time_asked_contacts";

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

    public void setShowOnboarding(boolean shouldShowOnboarding) {
        sharedPreferences.edit().putBoolean(PREF_SHOW_ONBOARDING, shouldShowOnboarding).apply();
    }

    public boolean shouldShowOnBoarding() {
        return sharedPreferences.getBoolean(PREF_SHOW_ONBOARDING, true);
    }

    public void setShouldUploadContacts(boolean shouldUploadContacts) {
        sharedPreferences.edit().putBoolean(PREF_UPLOAD_CONTACTS, shouldUploadContacts).apply();
    }

    public boolean shouldUploadContacts() {
        return sharedPreferences.getBoolean(PREF_UPLOAD_CONTACTS, false);
    }

    public void setUploadContactsAsked() {
        sharedPreferences.edit().putLong(PREF_LAST_TIME_ASKED_CONTACTS, System.currentTimeMillis()).apply();
    }

    public long getLastUploadContactsAsked () {
        return sharedPreferences.getLong(PREF_LAST_TIME_ASKED_CONTACTS, 0);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        new BackupManager(context).dataChanged();
    }
}
