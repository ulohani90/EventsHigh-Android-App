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
    public static final String PREF_LAST_TIME_REFER_SHOWN = "last_time_refer_shown";
    public static final String PREF_INTEREST_UPDATED = "interest_updated";
    public static final String PREF_SHOW_REFERRAL = "show_referral";
    public static final String PREF_OFFER_ACTED_ID = "offers_acted";


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

    public void setCanUploadContacts(boolean shouldUploadContacts) {
        sharedPreferences.edit().putBoolean(PREF_UPLOAD_CONTACTS, shouldUploadContacts).apply();
    }

    public boolean canUploadContacts() {
        return sharedPreferences.getBoolean(PREF_UPLOAD_CONTACTS, false);
    }

    public void setUploadContactsAsked() {
        sharedPreferences.edit().putLong(PREF_LAST_TIME_ASKED_CONTACTS, System.currentTimeMillis()).apply();
    }

    public long getLastUploadContactsAsked () {
        return sharedPreferences.getLong(PREF_LAST_TIME_ASKED_CONTACTS, 0);
    }

    public void setLastTimeReferShown() {
        sharedPreferences.edit().putLong(PREF_LAST_TIME_REFER_SHOWN, System.currentTimeMillis()).apply();
    }

    public long getLastTimeReferShown () {
        return sharedPreferences.getLong(PREF_LAST_TIME_REFER_SHOWN, 0);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        new BackupManager(context).dataChanged();
    }

    public void setIsInterestUpdated(boolean isInterestUpdated) {
        sharedPreferences.edit().putBoolean(PREF_INTEREST_UPDATED, isInterestUpdated).apply();
    }

    public boolean isInterestUpdated() {
        return sharedPreferences.getBoolean(PREF_INTEREST_UPDATED, false);
    }

    public void setShowReferral(boolean shouldShowReferal) {
        sharedPreferences.edit().putBoolean(PREF_SHOW_REFERRAL, shouldShowReferal).apply();
    }

    public boolean shouldShowReferral() {
        return sharedPreferences.getBoolean(PREF_SHOW_REFERRAL, false);
    }

    public void setPrefOfferActedId(String id){
        sharedPreferences.edit().putString(PREF_OFFER_ACTED_ID, id).apply();
    }
    public String getPrefOfferActedId(){
        return sharedPreferences.getString(PREF_OFFER_ACTED_ID, "");
    }
}
