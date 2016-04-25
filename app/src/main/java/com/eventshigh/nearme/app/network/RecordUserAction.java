package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.user.UserActionHelper;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by umesh on 25/04/16.
 */
public class RecordUserAction extends JsonObjectRequest{
    public static void submit(Context context, String action,String data,
                              Priority priority, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        JSONObject jsonRequest = new JSONObject();

        try {
            jsonRequest.put(UserActionHelper.JSON_KEY_ANDROID_ID, Utils.getAndroidId(context));
            jsonRequest.put(UserActionHelper.JSON_KEY_TIMESTAMP, Long.toString(System.currentTimeMillis()));
            jsonRequest.put(UserActionHelper.JSON_KEY_ACTION, action);
            jsonRequest.put(UserActionHelper.JSON_KEY_DATA, data);

        } catch (JSONException e) {
            errorListener.onErrorResponse(new VolleyError("Failed to create contacts json", e));
            return;
        }

        String url = UpdateAccountInfoService.getBaseUriWithoutAndroidId("record_user_action").build().toString();
        RecordUserAction request = new RecordUserAction(
                url, jsonRequest, priority, listener, errorListener);
        request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleyHelper.addToRequestQueue(context, request);
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
