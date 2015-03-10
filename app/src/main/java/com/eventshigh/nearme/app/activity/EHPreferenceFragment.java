package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Preferences;

public class EHPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // Add 'account' preferences.
        PreferenceCategory accountHeader = new PreferenceCategory(getActivity());
        accountHeader.setTitle(R.string.pref_header_account);
        getPreferenceScreen().addPreference(accountHeader);
        addPreferencesFromResource(R.xml.pref_account);

        // Add 'notifications' preferences, and a corresponding header.
        PreferenceCategory notificationHeader = new PreferenceCategory(getActivity());
        notificationHeader.setTitle(R.string.pref_header_notifications);
        getPreferenceScreen().addPreference(notificationHeader);
        addPreferencesFromResource(R.xml.pref_notification);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
        SettingsActivity.bindPreferenceSummaryToValue(
                findPreference(Preferences.PREF_DEFAULT_ACTIVITY));
    }
}
