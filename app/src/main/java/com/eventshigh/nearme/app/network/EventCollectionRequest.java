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
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.data.EventsCollection;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Volley Request to fetch Events collections.
 */
public class EventCollectionRequest extends JsonRequest<EventsCollection> {
    public static boolean shouldBypassCache = false;

    /**
     * Helper method to submit a volley request to fetch Events information.
     *
     * @param context an application context to initiate the volley.
     * @param param EventFetcherParam representing the request.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, EventFetcherParam param,
                              Priority priority, Object tag,
                              Listener<EventsCollection> listener, ErrorListener errorListener) {
        if (param.city == null) {
            errorListener.onErrorResponse(new VolleyError("No City for: " + param.toString()));
            return;
        }

        String url;
        if (param.query.isEmpty()) {
            url = EventsHighEndpoints.getApiEndpointDate(param.city);
        } else {
            try {
                url = EventsHighEndpoints.getApiEndpointQuery(param.city, param.query);
            } catch (UnsupportedEncodingException e) {
                errorListener.onErrorResponse(new VolleyError("Invalid Query", e));
                return;
            }
        }

        if (shouldBypassCache) {
            url = url + "&cmode=bypass";
        }

        EventCollectionRequest request = new EventCollectionRequest(context, url, param, priority,
                listener, errorListener);
        if (tag != null) {
            request.setTag(tag);
        }
        Helper.addToRequestQueue(context, request);
    }

    private final Context context;
    private final EventFetcherParam param;
    private final Priority priority;

    /**
     * Creates a new request.
     *
     * @param context application context.
     * @param url URL to fetch the JSON from.
     * @param priority priority of request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public EventCollectionRequest(Context context, String url, EventFetcherParam param, Priority priority,
                                  Listener<EventsCollection> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        this.context = context;
        this.param = param;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<EventsCollection> parseNetworkResponse(NetworkResponse response) {
        new ReportTimingTask(context, getUrl()).execute(response.networkTimeMs);

        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            JSONObject eventsJson = new JSONObject(jsonString);
            return Response.success(Event.parseUpcomingEvents(param, eventsJson),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException e) {
            return Response.error(new ParseError(e));
        }
    }
}
