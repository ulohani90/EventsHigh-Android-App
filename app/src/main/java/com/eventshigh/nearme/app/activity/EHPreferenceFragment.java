package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ListView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Account.UserCityListener;
import com.eventshigh.nearme.app.utils.Utils;

public class EHPreferenceFragment extends PreferenceFragment
    implements UserCityListener, Preference.OnPreferenceChangeListener {

    private ListPreference lastCityView;

    private Account account;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // Last city preference.
      /*  lastCityView = (ListPreference) getPreferenceScreen().findPreference("last_city_key");
        String[] cityNames = City.getValuesAsString();
        lastCityView.setEntries(cityNames);
        lastCityView.setEntryValues(cityNames);
        lastCityView.setOnPreferenceChangeListener(this);*/



        // shared preferences instance.
        account = new Account(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        View rootView = getView();
        if(rootView!=null) {
            ListView list = (ListView) rootView.findViewById(android.R.id.list);
            list.setDivider(null);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
       // onUserCityChanged(account.getLastCity());
       // Account.setUserCityListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

       // Account.setUserCityListener(null);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == lastCityView) {
            if (!newValue.equals(lastCityView.getValue())) {
                City newCity = City.getCity((String) newValue);
                 if (newCity != null) {
                    account.setLastCity(newCity);
                    LaunchActivity activity = (LaunchActivity) getActivity();
                    activity.cityChanged(newCity);
                }
            }
        }
        return true;
    }

    @Override
    public void onUserCityChanged(@Nullable City newUserCity) {

        //Remove dividers from the preferences
        if (newUserCity != null) {
            lastCityView.setValue(Utils.capitalize(newUserCity.name()));
            lastCityView.setSummary(Utils.capitalize(newUserCity.name()));
        }
    }
}
