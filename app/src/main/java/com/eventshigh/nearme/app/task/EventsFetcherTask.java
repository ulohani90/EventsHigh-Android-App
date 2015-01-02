package com.eventshigh.nearme.app.task;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventFetcherParam;
import com.eventshigh.nearme.app.utils.EventsCollection;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.StreamUtils;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * An {@link AsyncTask} which is used to fetch the Events. Once events are available,
 * the callback is called on UI thread. The Caller can then use Callback to refresh the events.
 *
 * Before using EventsFetcher, its recommended to setup {@link android.net.http.HttpResponseCache}
 * for better performance.
 */
public class EventsFetcherTask extends AsyncTask<EventFetcherParam, Void, EventsCollection> {
    private static final String LOG_TAG = EventsFetcherTask.class.getSimpleName();

    public static interface EventsFetcherCallBack {
        public void OnEventsAvailable(EventFetcherParam param, EventsCollection events);
    }

    private final Activity activity;
    private final boolean shouldOverrideCache;
    private final EventsFetcherCallBack callback;
    private ProgressDialog pDialog;
    private EventFetcherParam param;

    public EventsFetcherTask(Activity activity, boolean shouldOverrideCache,
                             EventsFetcherCallBack callback) {
        this.activity = activity;
        this.shouldOverrideCache = shouldOverrideCache;
        this.callback = callback;
    }

    public void destroy() {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
            pDialog = null;
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pDialog = new ProgressDialog(activity);
        pDialog.setMessage(activity.getString(R.string.loading));
        pDialog.setCancelable(false);
        pDialog.show();
    }

    @Override
    protected EventsCollection doInBackground(EventFetcherParam... params) {
        param = params[0];

        if (param.city == null) {
            Log.w(LOG_TAG, "No City for: " + param.toString());
            return null;
        }

        String url;
        if (param.query.isEmpty()) {
            url = EventsHighEndpoints.getApiEndpointDate(
                    param.city, DateTimeUtils.getDate(param.day));
        } else {
            try {
                url = EventsHighEndpoints.getApiEndpointQuery(param.city, param.query);
            } catch (UnsupportedEncodingException e) {
                Log.w(LOG_TAG, "Invalid Query", e);
                return null;
            }
        }

        if (shouldOverrideCache) {
            url = url + "?cmode=bypass";
        }

        try {
            return Event.parseUpcomingEvents(param.city, StreamUtils.fetchJSON(url));
        } catch (JSONException|IOException e) {
            Log.e(LOG_TAG, "Failed to fetch events!", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(@Nullable EventsCollection result) {
        super.onPostExecute(result);

        if (activity.isFinishing()) {
            return;
        }

        destroy();

        if (result == null) {
            // Failed. Show toast and return empty list.
            Toast.makeText(activity, R.string.failed_load, Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.getTags().isEmpty()) {
            // Failed. Show toast and return empty list.
            Toast.makeText(activity, R.string.no_events, Toast.LENGTH_SHORT).show();
            return;
        }

        callback.OnEventsAvailable(param, result);
    }
}
