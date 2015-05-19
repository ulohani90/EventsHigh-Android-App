package com.eventshigh.nearme.app.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;

public class EHPreferenceFragment extends PreferenceFragment
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Preference pointsView;

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

        // Num Points preference.
        pointsView = getPreferenceScreen().findPreference("points_key");
        pointsView.setSummary(Preferences.getInstance(getActivity()).getPoints());
        Account.getNumPoints(getActivity(), null);

        Preferences.getInstance(getActivity()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Preferences.PREF_POINTS.equals(key) && isAdded()) {
            pointsView.setSummary(Preferences.getInstance(getActivity()).getPoints());
        }
    }
}
