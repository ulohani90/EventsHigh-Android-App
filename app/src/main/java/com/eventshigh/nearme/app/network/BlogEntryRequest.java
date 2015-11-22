package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.eventshigh.nearme.app.data.BlogEntry;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

public class BlogEntryRequest extends JsonRequest<BlogEntry> {
    /**
     * Helper method to submit a volley request to fetch blog entry from its Uri.
     *
     * @param context an application context to initiate the volley.
     * @param blogUri Uri representing the blog.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, String blogUri, Priority priority, boolean shouldBypassCache,
                              Listener<BlogEntry> listener, ErrorListener errorListener) {
        String url = EventsHighEndpoints.getApiEndpointBlogEntry(blogUri);
        BlogEntryRequest request = new BlogEntryRequest(context, url, priority, shouldBypassCache,
                listener, errorListener);
        request.setTag(context);
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final Context context;
    private final Priority priority;

    /**
     * Creates a new request.
     * @param url URL to fetch the JSON from
     * @param priority priority of request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public BlogEntryRequest(Context context, String url, Priority priority, boolean shouldBypassCache,
                        Listener<BlogEntry> listener, ErrorListener errorListener) {
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
    protected Response<BlogEntry> parseNetworkResponse(NetworkResponse response) {
        ReportTimingTask.report(context, "blogEntry", response.networkTimeMs);

        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            JSONObject blogEntryJson = new JSONObject(jsonString);
            return Response.success(BlogEntry.parse(blogEntryJson.getJSONObject("post")),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException | ParseException e) {
            return Response.error(new ParseError(e));
        }
    }
}
