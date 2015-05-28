package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
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
import com.crashlytics.android.Crashlytics;
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
    private static final String PREF_LAST_UPLOAD_TIMESTAMP = "lastUploadTimestamp";

    private Intent intent;
    private SharedPreferences preferences;
    private long uploadTimestamp;

    public UploadUserActionsService() {
        super("UploadUserActionsService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.intent = intent;
        this.uploadTimestamp = System.currentTimeMillis();

        recordUserActions();
    }

    private void recordUserActions() {
        try {
            long lastUploadTimestamp = preferences.getLong(PREF_LAST_UPLOAD_TIMESTAMP, 0);
            long aWeekBack = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * 7;
            if (lastUploadTimestamp < aWeekBack) {
                lastUploadTimestamp = aWeekBack;
            }

            final JSONObject postBody = UserActionDbHelper.getInstance(this)
                    .getActionsSince(lastUploadTimestamp);
            if (postBody != null) {
                Uri uri = AccountStateReporter.getBaseUri(this, "record_user_action").build();
                String signedUri = Signer.sign(uri).toString();
                VolleyHelper.addToRequestQueue(this,
                    new JsonObjectRequest(Request.Method.POST, signedUri, postBody, onSuccess, onError));
            }
        } catch (IOException | GeneralSecurityException | JSONException e) {
            Log.w(LOG_TAG, "Failed to upload user actions", e);
            Crashlytics.logException(e);
            cleanUp();
        }
    }

    private void cleanUp() {
        NetworkChangeBroadcastReceiver.completeWakefulIntent(intent);
    }

    private Response.Listener<JSONObject> onSuccess = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject jsonObject, boolean isIntermediate) {
            // Mark the location up to which the data has been uploaded
            preferences.edit().putLong(PREF_LAST_UPLOAD_TIMESTAMP, uploadTimestamp).apply();
            Log.i(LOG_TAG, "Successfully uploaded user actions");
            cleanUp();
        }
    };

    Response.ErrorListener onError = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            Log.w(LOG_TAG, "Failed to uploaded user actions", volleyError.getCause());
            Crashlytics.logException(volleyError.getCause());
            cleanUp();
        }
    };
}
