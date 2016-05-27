package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.BlogEntry;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class BlogFeedRequest extends JsonRequest<List<BlogEntry>> {
    /**
     * Helper method to submit a volley request to fetch blog entries.
     *
     * @param context an application context to initiate the volley.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, Priority priority, boolean shouldBypassCache,
                              Listener<List<BlogEntry>> listener, ErrorListener errorListener) {
        City city = new Account(context).getLastCity();
        if (city == null) {
            errorListener.onErrorResponse(new VolleyError("user city is unknown"));
        } else {
            submit(context, city, priority, shouldBypassCache, listener, errorListener);
        }
    }

    public static void submit(Context context, City city, Priority priority, boolean shouldBypassCache,
                              Listener<List<BlogEntry>> listener, ErrorListener errorListener) {
        String url = EventsHighEndpoints.getApiEndpointBlogFeed(city);
        BlogFeedRequest request = new BlogFeedRequest(context, url, priority, shouldBypassCache,
                listener, errorListener);
        request.setTag(context);
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final Context context;
    private final Priority priority;

    /**
     * Creates a new request.
     *
     * @param url URL to fetch the JSON from
     * @param priority priority of request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public BlogFeedRequest(Context context, String url, Priority priority, boolean shouldBypassCache,
                           Listener<List<BlogEntry>> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);

        this.context = context;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<List<BlogEntry>> parseNetworkResponse(NetworkResponse response) {
        ReportTimingTask.report(context, "blogFeed", response.networkTimeMs);

        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            JSONObject blogEntriesJson = new JSONObject(jsonString);
            return Response.success(BlogEntry.parse(blogEntriesJson.getJSONArray("posts")),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }

}
