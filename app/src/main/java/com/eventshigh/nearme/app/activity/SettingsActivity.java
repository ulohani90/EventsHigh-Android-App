package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;
import com.eventshigh.nearme.app.utils.GAHelper;

/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 *
 * See http://developer.android.com/guide/topics/ui/settings.html.
 */
public class SettingsActivity extends Activity {
    protected void onResume() {
        super.onResume();

        GAHelper gaHelper = GAHelper.getInstance(this);
        Intent intent  = getIntent();
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("com.eventshigh.delete_query_history")) {
                gaHelper.reportActionToAnalytics(SettingsActivity.class.getSimpleName(), "deleteQueryHistory");

                EventSearchSuggestionsProvider.clearHistory(this);
                Toast.makeText(this, R.string.message_delete_query_history, Toast.LENGTH_SHORT).show();
            }

            if (intent.getAction().equals("com.eventshigh.restore_events")) {
                gaHelper.reportActionToAnalytics(SettingsActivity.class.getSimpleName(), "restoreEvents");

                EventsMarkerManager.getInstance(this).restoreAll();
                Toast.makeText(this, R.string.message_restore_event, Toast.LENGTH_SHORT).show();
            }
        }
            finish();
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    public static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
        new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();

                if (preference instanceof ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);
                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    preference.setSummary(stringValue);
                }
                return true;
            }
        };

}
