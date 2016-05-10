package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;

import com.android.volley.Response;

import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;

import com.eventshigh.nearme.app.data.MovieDetailObject;

import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

/**
 * Created by umesh on 05/05/16.
 */
public class MovieDetailRequest extends JsonRequest<MovieDetailObject> {
    /**
     * Helper method to submit a volley request to fetch Event information from its Uri.
     *
     * @param context       an application context to initiate the volley.
     * @param listener      callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, int id, Priority priority,
                              Listener<MovieDetailObject> listener, ErrorListener errorListener) {


        String url = EventsHighEndpoints.getApiEndpointForMovieUberDetail(id);

        MovieDetailRequest request = new MovieDetailRequest(context, url, priority, listener, errorListener);
        request.setTag(context);
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final Context context;
    private final Priority priority;

    /**
     * Creates a new request.
     *
     * @param url           URL to fetch the JSON from
     * @param priority      priority of request.
     * @param listener      Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public MovieDetailRequest(Context context, String url, Priority priority,
                              Listener<MovieDetailObject> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        this.context = context;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<MovieDetailObject> parseNetworkResponse(NetworkResponse response) {


        try {
            String jsonString = new String(response.data, "UTF-8");

            JSONObject obj = new JSONObject(jsonString);
            return Response.success(new MovieDetailObject(context, obj),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {

            return Response.error(new ParseError(e));
        }
    }
}
