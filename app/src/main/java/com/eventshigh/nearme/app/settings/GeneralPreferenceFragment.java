package com.eventshigh.nearme.app.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.eventshigh.nearme.app.R;

/**
 * This fragment shows general preferences.
 */
public class GeneralPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pref_general);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.
        SettingsActivity.bindPreferenceSummaryToValue(
                findPreference(Preferences.PREF_DEFAULT_ACTIVITY));
    }
}
