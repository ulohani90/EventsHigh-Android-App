package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;

import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MovieMarkerManager;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by umesh on 17/05/16.
 */
public class MultiMovieRequest extends JsonRequest<List<MovieDetailObject>> {


    List<String> movieIds;

    /**
     * Helper method to submit a volley request to fetch Events information.
     *
     * @param listener      callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, EventsContext eventsContext, List<String> movieIds,
                              Priority priority, Object tag, boolean shouldBypassCache, boolean includeWithoutLocation,
                              Response.Listener<List<MovieDetailObject>> listener, Response.ErrorListener errorListener) {


        if (movieIds.isEmpty()) {
            listener.onResponse(new ArrayList<MovieDetailObject>(), false);
            return;
        }

        String url;
        try {
            url = EventsHighEndpoints.getApiEndpointMoviesUber(movieIds, (new Account(context)).getLastCity().name());
        } catch (IllegalArgumentException e) {
            errorListener.onErrorResponse(new VolleyError("Invalid Query", e));
            return;
        }

        MultiMovieRequest request = new MultiMovieRequest(context, eventsContext, url, movieIds, priority,
                shouldBypassCache, includeWithoutLocation, listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final EventsContext eventsContext;
    private final Priority priority;
    private final boolean includeWithoutLocation;
    private Context mContext;

    public MultiMovieRequest(Context context, EventsContext eventsContext, String url, List<String> movieIds, Priority priority,
                             boolean shouldBypassCache, boolean includeWithoutLocation,
                             Response.Listener<List<MovieDetailObject>> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(true);
        this.movieIds = movieIds;
        this.eventsContext = eventsContext;
        this.priority = priority;
        this.includeWithoutLocation = includeWithoutLocation;
        this.mContext = context;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<List<MovieDetailObject>> parseNetworkResponse(NetworkResponse response) {
        try {
            List<MovieDetailObject> movieDetailObjects = new ArrayList<>();

            String jsonString = new String(response.data, "UTF-8");
            JSONObject eventsJson = new JSONObject(jsonString);
            Iterator<String> keys = eventsJson.keys();

            List<String> movieKeys = removeUnusedKeys(keys);

            for (String key : movieKeys) {
                try {
                    MovieDetailObject movieDetailObject = new MovieDetailObject(mContext, eventsJson.getJSONObject(key));
                    movieDetailObjects.add(movieDetailObject);
                } catch (JSONException e) {
                    Crashlytics.getInstance().core.logException(e);
                }
            }

            // Sort the event list to user.

            return Response.success(movieDetailObjects, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }

    public List<String> removeUnusedKeys(Iterator<String> keys) {
        List<String> ids = new ArrayList<>();
        while (keys.hasNext()) {
            ids.add(keys.next());
        }
        for (String movieId : movieIds) {
            if (!ids.contains(movieId)) {
                MovieMarkerManager.Editor eventsMarkerEditor =
                        MovieMarkerManager.getInstance(mContext).getEditor();
                eventsMarkerEditor.removeMovieMark(movieId);
                eventsMarkerEditor.close();
            }
        }
        return ids;
    }
}
