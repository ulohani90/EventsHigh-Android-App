package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;

import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.UserActionHelper;
import com.eventshigh.nearme.app.utils.Signer;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Created by umesh on 25/04/16.
 */
public class RecordUserAction extends JsonObjectRequest {
    public static void submit(Context context, String action, JSONObject data,
                              Priority priority, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        JSONObject jsonRequest = new JSONObject();

        try {
            jsonRequest.put(UserActionHelper.JSON_KEY_ANDROID_ID, Utils.getAndroidId(context));
            JSONArray actions = new JSONArray();

            JSONObject actionObj = new JSONObject();
            actionObj.put(UserActionHelper.JSON_KEY_TIMESTAMP, Long.toString(System.currentTimeMillis()));
            actionObj.put(UserActionHelper.JSON_KEY_ACTION, action);
            actionObj.put(UserActionHelper.JSON_KEY_DATA, data);
            actionObj.put("lat", 0);
            actionObj.put("lon", 0);

            actions.put(actionObj);

            jsonRequest.put("actions", actions);

        } catch (JSONException e) {
            errorListener.onErrorResponse(new VolleyError("Failed to create contacts json", e));
            return;
        }
        try {
            Uri uri = UpdateAccountInfoService.getBaseUriWithoutAndroidId("record_user_action").build();
            RecordUserAction request = new RecordUserAction(
                    Signer.sign(uri).toString(), jsonRequest, priority, listener, errorListener);

            request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyHelper.addToRequestQueue(context, request);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private final Priority priority;

    public RecordUserAction(String url, JSONObject jsonRequest, Priority priority,
                            Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(Request.Method.POST, url, jsonRequest, listener, errorListener);
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }
}
