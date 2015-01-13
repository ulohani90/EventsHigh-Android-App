package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

/**
 * Volley Request to fetch the a single event data.
 */
public class EventRequest extends JsonRequest<Event> {
    /**
     * Helper method to submit a volley request to fetch Event information from its Uri.
     *
     * @param context an application context to initiate the volley.
     * @param eventUri Uri representing the event.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, Uri eventUri, Priority priority, @Nullable Object tag,
                              Listener<Event> listener, ErrorListener errorListener) {
        List<String> eventUriPathSegments = eventUri.getPathSegments();
        if (eventUriPathSegments.size() < 2) {
            errorListener.onErrorResponse(new VolleyError("Invalid data: " + eventUri));
            return;
        }

        City city;
        try {
            city = City.valueOf(
                    eventUriPathSegments.get(eventUriPathSegments.size() - 2).toUpperCase());
        } catch (IllegalArgumentException e) {
            errorListener.onErrorResponse(new VolleyError("Invalid data, no city: " + eventUri, e));
            return;
        }

        String eventId = eventUriPathSegments.get(eventUriPathSegments.size() - 1).split("-", 2)[0];
        String url = EventsHighEndpoints.getApiEndpointEventUber(eventId);
        EventRequest request = new EventRequest(context, url, city, priority,
                listener, errorListener);
        if (tag != null) {
            request.setTag(tag);
        }
        Helper.addToRequestQueue(context, request);
    }

    private final Context context;
    private final City city;
    private final Priority priority;

    /**
     * Creates a new request.
     * @param url URL to fetch the JSON from
     * @param priority priority of request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public EventRequest(Context context, String url, City city, Priority priority,
                        Listener<Event> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        this.context = context;
        this.city = city;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<Event> parseNetworkResponse(NetworkResponse response) {
        new ReportTimingTask(context, getUrl()).execute(response.networkTimeMs);

        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            JSONObject eventJson = new JSONObject(jsonString);
            return Response.success(Event.fromJSON(city, eventJson),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException | ParseException e) {
            return Response.error(new ParseError(e));
        }
    }
}
