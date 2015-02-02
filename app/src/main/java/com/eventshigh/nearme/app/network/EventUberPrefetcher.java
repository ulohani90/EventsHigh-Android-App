package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to pre fetch the event data. This class send the request to fetch
 * event data and discards the result. The Volley cache should help in getting the
 * event data next time when user needs it.
 */
public class EventUberPrefetcher {
    private static final int CACHE_SIZE = 100;

    private static EventUberPrefetcher instance;
    public static synchronized EventUberPrefetcher getInstance(Context context) {
        if (instance == null) {
            instance = new EventUberPrefetcher(context);
        }
        return instance;
    }

    private final Context context;
    private final Set<String> cachedEventId = Collections.newSetFromMap(
            new LinkedHashMap<String, Boolean>() {
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > CACHE_SIZE;
                }
            });

    public EventUberPrefetcher(Context context) {
        this.context = context;
    }

    public void prefetch(String eventId) {
        synchronized (cachedEventId) {
            if (!cachedEventId.add(eventId)) {
                // already cached. do nothing.
                return;
            }
        }

        String url = EventsHighEndpoints.getApiEndpointEventUber(eventId);
        StringRequest request = new StringRequest(url, NULL_LISTENER, NULL_ERROR_LISTENER);
        VolleyHelper.addToRequestQueue(context, request);
    }

    private static final Listener<String> NULL_LISTENER = new Listener<String>() {
        @Override
        public void onResponse(String s, boolean isIntermediate) {
            // do nothing.
        }
    };

    private static final ErrorListener NULL_ERROR_LISTENER = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            // do nothing.
        }
    };
}
