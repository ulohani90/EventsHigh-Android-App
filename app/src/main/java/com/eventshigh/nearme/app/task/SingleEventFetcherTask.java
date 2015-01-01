package com.eventshigh.nearme.app.task;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.StreamUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
* Fetches a single event data.
*/
public class SingleEventFetcherTask extends AsyncTask<Uri, Void, Event> {
    public static interface OnEventFetchHandler {
        public void onEventFetch(Event event);
    }

    private static final String LOG_TAG = SingleEventFetcherTask.class.getSimpleName();

    private final Activity activity;
    private final OnEventFetchHandler handler;

    public SingleEventFetcherTask(Activity activity, OnEventFetchHandler handler) {
        this.activity = activity;
        this.handler = handler;
    }

    @Override
    protected Event doInBackground(Uri... params) {
        Uri eventUri = params[0];
        List<String> eventUriPathSegments = eventUri.getPathSegments();

        if (eventUriPathSegments.size() < 2) {
            Log.w(LOG_TAG, "Invalid data: " + eventUri);
            return null;
        }

        City city;
        try {
            city = City.valueOf(
                    eventUriPathSegments.get(eventUriPathSegments.size() - 2).toUpperCase());
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, "Invalid data, no city: " + eventUri, e);
            return null;
        }

        String eventId = eventUriPathSegments.get(eventUriPathSegments.size() - 1).split("-", 2)[0];
        String url = EventsHighEndpoints.getApiEndpointEventUber(eventId);
        try {
            JSONObject eventJSON = StreamUtils.fetchJSON(url);
            return Event.fromJSON(city, eventJSON);
        } catch (IOException | JSONException | ParseException e) {
            Log.w(LOG_TAG, "Failed to fetch: " + url, e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(@Nullable Event result) {
        super.onPostExecute(result);

        if (result == null) {
            // Failed. Show toast and return empty list.
            Toast.makeText(activity, R.string.failed_load, Toast.LENGTH_SHORT).show();
            activity.finish();
            return;
        }

        handler.onEventFetch(result);
    }
}
