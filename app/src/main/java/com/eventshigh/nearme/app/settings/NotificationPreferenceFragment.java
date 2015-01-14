package com.eventshigh.nearme.app.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.eventshigh.nearme.app.R;

/**
 * This fragment shows notification preferences.
 */
public class NotificationPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_notification);
    }
}
