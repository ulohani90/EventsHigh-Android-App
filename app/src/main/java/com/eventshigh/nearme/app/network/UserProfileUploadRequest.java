package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author shubham
 * @since 20/5/16.
 */



public class UserProfileUploadRequest extends JsonObjectRequest {

    public static void submit(Context context,JSONObject data,
                              Priority priority, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {


        try {
            URL url = new URL(EventsHighEndpoints.API_URI_BASE +"reviews/add");
            Uri uri = Uri.parse(url.toURI().toString());
            UserProfileUploadRequest request = new UserProfileUploadRequest(
                    uri.toString(), data, priority, listener, errorListener);

            request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyHelper.addToRequestQueue(context, request);
        }catch(MalformedURLException e){
            Log.e("Profile Info Error", e.toString());
        }catch (URISyntaxException e){
            Log.e("Profile Info Error",e.toString());
        }
    }

    private final Priority priority;

    public UserProfileUploadRequest(String url, JSONObject jsonRequest, Priority priority,
                                    Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(Request.Method.POST, url, jsonRequest, listener, errorListener);
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

}
