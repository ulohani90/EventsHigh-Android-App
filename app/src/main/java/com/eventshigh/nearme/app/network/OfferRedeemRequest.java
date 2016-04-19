package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by umesh on 17/04/16.
 */
public class OfferRedeemRequest extends JsonObjectRequest{

    public static void submit(Context context, List<UserContact> contacts,
                              Priority priority, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        JSONObject jsonRequest = new JSONObject();
        JSONArray userContactArray = new JSONArray();
        try {
            jsonRequest.put("android_id", Utils.getAndroidId(context));
            jsonRequest.put("contacts", userContactArray);
            for (UserContact contact : contacts) {
                userContactArray.put(contact.toJSON());
            }
        } catch (JSONException e) {
            errorListener.onErrorResponse(new VolleyError("Failed to create contacts json", e));
            return;
        }

        String url = UpdateAccountInfoService.getBaseUriWithoutAndroidId("record_user_contacts").build().toString();
        ContactsUploadRequest request = new ContactsUploadRequest(
                url, jsonRequest, priority, listener, errorListener);
        request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final Priority priority;

    public OfferRedeemRequest(String url, JSONObject jsonRequest, Priority priority,
                                 Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(Request.Method.POST, url, jsonRequest, listener, errorListener);
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }
}
