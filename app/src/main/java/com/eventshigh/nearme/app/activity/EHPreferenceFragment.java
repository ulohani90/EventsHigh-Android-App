package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import com.eventshigh.nearme.app.R;

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
    }
}
