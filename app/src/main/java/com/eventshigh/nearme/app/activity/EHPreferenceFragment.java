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

        // Disabled for now ... we need to build a screen to show points information.
        /**
        final Preference pointsView = getPreferenceScreen().findPreference("points_key");
        pointsView.setSummary(Preferences.getInstance(getActivity()).getPoints());

        Pair<String, Boolean> phoneNumberStatus = new Account(getActivity()).getPhoneNumber();
        if (Utils.isDebug(getActivity()) || phoneNumberStatus.second) {
            Uri requestUrl = AccountStateReporter.getBaseUri(getActivity(), "get_user_points")
                .appendQueryParameter("mobile_no", phoneNumberStatus.first)
                .build();
            try {
                VolleyHelper.addToRequestQueue(getActivity(),
                    new JsonObjectRequest(Request.Method.GET, Signer.sign(requestUrl).toString(), null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response, boolean isIntermediate) {
                                try {
                                    Preferences.getInstance(getActivity()).setPoints(
                                        response.getString("total_points"));
                                    pointsView.setSummary(Preferences.getInstance(
                                        getActivity()).getPoints());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                            }
                        }
                    )
                );
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
       **/
    }
}
