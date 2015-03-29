package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
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
    private Intent intent;

    public UploadUserActionsService() {
        super("UploadUserActionsService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        this.intent = intent;
        recordUserActions(this, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject, boolean b) {
                // TODO: mark tha location up to which the data has been uploaded
                cleanUp();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                cleanUp();
            }
        });
    }

    private void recordUserActions(Context context, final Response.Listener<JSONObject> onSuccess,
                                   final Response.ErrorListener onFailed) {
        Uri uri = AccountStateReporter.getBaseUri(context, "mobile_app_users")
                .appendPath("record_user_action")
                .build();
        try {
            final String postBody = UserActionDbHelper.getInstance(this).getActionsSince(0);
            VolleyHelper.addToRequestQueue(context, new JsonObjectRequest(Request.Method.POST,
                            Signer.sign(uri).toString(), null, onSuccess, onFailed) {
                        @Override
                        public byte[] getBody() {
                            return postBody.getBytes();
                        }
                    }
            );
        } catch (IOException | GeneralSecurityException | JSONException e) {
            Log.w(AccountStateReporter.class.getSimpleName(),
                    "Failed to upload user actions: " + uri, e);
            cleanUp();
        }
    }

    private void cleanUp() {
        // Remove the mark that says upload is in progress
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putBoolean(NetworkChangeBroadcastReceiver.PREF_IS_UPLOADING, false).apply();
        NetworkChangeBroadcastReceiver.completeWakefulIntent(intent);
    }
}
