package com.eventshigh.nearme.app.activity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Pair;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

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

        Preferences.getInstance(getActivity()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Preferences.PREF_POINTS.equals(key)) {
            pointsView.setSummary(Preferences.getInstance(getActivity()).getPoints());
        }
    }
}
