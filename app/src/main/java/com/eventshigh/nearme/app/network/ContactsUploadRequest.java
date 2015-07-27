package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Request to upload user contacts.
 */
public class ContactsUploadRequest extends JsonObjectRequest {

    public static void submit(Context context, List<UserContact> contacts,
            Priority priority, Listener<JSONObject> listener, ErrorListener errorListener) {

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

        Log.d(ContactsUploadRequest.class.getSimpleName(), "request: " + jsonRequest.toString());
        String url = AccountStateReporter.getBaseUriWithoutAndroidId("record_user_contacts").build().toString();
        ContactsUploadRequest request = new ContactsUploadRequest(
                url, jsonRequest, priority, listener, errorListener);
        request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final Priority priority;

    public ContactsUploadRequest(String url, JSONObject jsonRequest, Priority priority,
                                 Listener<JSONObject> listener, ErrorListener errorListener) {
        super(Request.Method.POST, url, jsonRequest, listener, errorListener);
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }
}
