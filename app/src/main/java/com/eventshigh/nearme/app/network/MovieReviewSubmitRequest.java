package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.user.UserActionHelper;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Signer;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * @author shubham
 * @since 17/5/16.
 */


public class MovieReviewSubmitRequest extends JsonObjectRequest {

    public static void submit(Context context,JSONObject data,
                              Priority priority, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {


        try {
            URL url = new URL(EventsHighEndpoints.API_URI_BASE +"reviews/add");
            Uri uri = Uri.parse(url.toURI().toString());
            MovieReviewSubmitRequest request = new MovieReviewSubmitRequest(
                    uri.toString(), data, priority, listener, errorListener);

            request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyHelper.addToRequestQueue(context, request);
        }catch(MalformedURLException e){
            Log.e("Movie Review Error",e.toString());
        }catch (URISyntaxException e){
            Log.e("Movie Review Error",e.toString());
        }
    }

    private final Priority priority;

    public MovieReviewSubmitRequest(String url, JSONObject jsonRequest, Priority priority,
                            Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(Request.Method.POST, url, jsonRequest, listener, errorListener);
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

}
