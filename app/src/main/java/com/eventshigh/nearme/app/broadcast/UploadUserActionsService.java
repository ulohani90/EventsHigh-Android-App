package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.data.UserActionDbHelper;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.user.AccountStateReporter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class UploadUserActionsService extends IntentService {
    private static final String LOG_TAG = UploadUserActionsService.class.getSimpleName();

    private Intent intent;
    private SharedPreferences preferences;

    public UploadUserActionsService() {
        super("UploadUserActionsService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.intent = intent;
        final long uploadTimestamp = System.currentTimeMillis();
        recordUserActions(this, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject, boolean b) {
                // Mark the location up to which the data has been uploaded
                preferences.edit().putLong(
                        NetworkChangeBroadcastReceiver.PREF_LAST_UPLOAD_TIMESTAMP, uploadTimestamp)
                        .apply();
                Log.i(LOG_TAG, "Successfully uploaded user actions");
                cleanUp();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.w(LOG_TAG, "Failed to uploaded user actions");
                cleanUp();
            }
        });
    }

    private void recordUserActions(Context context, final Response.Listener<JSONObject> onSuccess,
                                   final Response.ErrorListener onFailed) {
        Uri uri = AccountStateReporter.getBaseUri(context, "record_user_action")
                .build();
        try {
            long lastUploadTimestamp = preferences.getLong(
                    NetworkChangeBroadcastReceiver.PREF_LAST_UPLOAD_TIMESTAMP, 0);
            long aWeekBack = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * 7;
            if (lastUploadTimestamp < aWeekBack) {
                lastUploadTimestamp = aWeekBack;
            }

            final String postBody = UserActionDbHelper.getInstance(this).getActionsSince(
                    lastUploadTimestamp);
            VolleyHelper.addToRequestQueue(context, new JsonObjectRequest(Request.Method.POST,
                            Signer.sign(uri).toString(), null, onSuccess, onFailed) {
                        @Override
                        public byte[] getBody() {
                            return postBody.getBytes();
                        }
                    }
            );
        } catch (IOException | GeneralSecurityException | JSONException e) {
            Log.w(LOG_TAG, "Failed to upload user actions: " + uri, e);
            cleanUp();
        }
    }

    private void cleanUp() {
        // Remove the mark that says upload is in progress
        preferences.edit().putBoolean(NetworkChangeBroadcastReceiver.PREF_IS_UPLOADING, false)
                .apply();
        NetworkChangeBroadcastReceiver.completeWakefulIntent(intent);
    }
}
