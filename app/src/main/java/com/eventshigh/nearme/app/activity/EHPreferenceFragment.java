package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.user.GcmRegistration.UserCityListener;
import com.eventshigh.nearme.app.utils.Utils;

public class EHPreferenceFragment extends PreferenceFragment
    implements UserCityListener, Preference.OnPreferenceChangeListener {

    private ListPreference lastCityView;

    private GcmRegistration gcmRegistration;

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

        // Last city preference.
        lastCityView = (ListPreference) getPreferenceScreen().findPreference("last_city_key");
        String[] cityNames = City.getValuesAsString();
        lastCityView.setEntries(cityNames);
        lastCityView.setEntryValues(cityNames);
        lastCityView.setOnPreferenceChangeListener(this);

        // shared preferences instance.
        gcmRegistration = GcmRegistration.getInstance(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        onUserCityChanged(gcmRegistration.getLastCity());
        gcmRegistration.setUserCityListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        gcmRegistration.setUserCityListener(null);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == lastCityView) {
            if (!newValue.equals(lastCityView.getValue())) {
                City newCity = City.getCity((String) newValue);
                 if (newCity != null) {
                    gcmRegistration.setLastCity(newCity, null);
                    LaunchActivity activity = (LaunchActivity) getActivity();
                    activity.cityChanged(newCity);
                }
            }
        }
        return true;
    }

    @Override
    public void onUserCityChanged(@Nullable City newUserCity) {
        if (newUserCity != null) {
            lastCityView.setValue(Utils.capitalize(newUserCity.name()));
            lastCityView.setSummary(Utils.capitalize(newUserCity.name()));
        }
    }
}
