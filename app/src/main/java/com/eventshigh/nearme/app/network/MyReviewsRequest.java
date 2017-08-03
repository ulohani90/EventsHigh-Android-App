package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 23/05/16.
 */
public class MyReviewsRequest extends JsonRequest<List<MovieUserReviewObject>> {

    public static void submit(Context context, String phoneNo, Priority priority,
                              Object tag, boolean shouldBypassCache, Response.Listener<List<MovieUserReviewObject>> listener,
                              Response.ErrorListener errorListener) {

        String url = EventsHighEndpoints.getApiEndpPointForMyReviews(phoneNo);
        if (shouldBypassCache) {
            url += "?cmode=bypass";
        }
        MyReviewsRequest request = new MyReviewsRequest(
                context, url, shouldBypassCache, priority, listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }


    Context context;
    Priority priority;

    public MyReviewsRequest(Context context, String url, boolean shouldBypassCache, Priority priority, Response.Listener<List<MovieUserReviewObject>> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(false);
        this.context = context;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<List<MovieUserReviewObject>> parseNetworkResponse(NetworkResponse response) {


        try {
            List<MovieUserReviewObject> objs = new ArrayList<>();
            String jsonString = new String(response.data, "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray reviews = jsonObject.getJSONArray("reviews");
            if (reviews != null) {
                for (int i = 0; i < reviews.length(); i++) {
                    objs.add(new MovieUserReviewObject(reviews.getJSONObject(i)));
                }
            }
            return Response.success(objs,
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (JSONException | UnsupportedEncodingException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }

    }
}
