package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Request to shorten an URI and get shortened URI from goo.gl
 */
public class URLShortenerRequest extends JsonRequest<String> {
    private static final String GoogleApiShortenerEndpoint =
            "https://www.googleapis.com/urlshortener/v1/url?key=AIzaSyCD_ciM1UH8-qp-KMzKVD-ICCXAEpC_PZY";
    private static final int TIMEOUT_MS = 1000;

    /**
     * Helper method to submit a volley request to fetch shortened URI.
     */
    public static void submit(Context context, String longUri, Listener<String> listener,
              ErrorListener errorListener) {
        URLShortenerRequest request = new URLShortenerRequest(longUri, listener, errorListener);
        request.setTag(context);
        request.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS, 1, 1));
        VolleyHelper.addToRequestQueue(context, request);
    }

    /**
     * Creates a new request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public URLShortenerRequest(String longUri, Listener<String> listener, ErrorListener errorListener) {
        super(Method.POST, GoogleApiShortenerEndpoint, getRequest(longUri), listener, errorListener);
    }

    @Override
    public Priority getPriority() {
        return Priority.IMMEDIATE;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            JSONObject resp = new JSONObject(jsonString);
            return Response.success(resp.getString("id"),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            return Response.error(new ParseError(e));
        }
    }

    private static String getRequest(String longUri) {
        return "{\"longUrl\": \"" + longUri +"\"}";
    }
}
