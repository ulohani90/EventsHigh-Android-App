package com.eventshigh.nearme.app.data;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsCollection.Builder;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * An {@link AsyncTask} which is used to fetch the Events. Once events are available,
 * the callback is called on UI thread. The Caller can then use Callback to refresh the events.
 *
 * Before using EventsFetcher, its recommended to setup {@link android.net.http.HttpResponseCache}
 * for better performance.
 */
public class EventsFetcher extends AsyncTask<EventFetcherParam, Void, EventsCollection> {

    private static final String LOG_TAG = EventsFetcher.class.getSimpleName();
    private static final String API_ENDPOINT_DATE =
            "http://apiserver.eventshigh.com:8888/api/date/CITY/DATE?sortby=popularity&limit=200&mobile=1";
    private static final String API_ENDPOINT_QUERY =
            "http://apiserver.eventshigh.com:8888/api/events/CITY/QUERY?sortby=popularity&limit=200&mobile=1";

    public static interface EventsFetcherCallBack {
        public void OnEventsAvailable(EventFetcherParam param, EventsCollection events);
    }

    private final Activity activity;
    private final EventsFetcherCallBack callback;
    private ProgressDialog pDialog;
    private EventFetcherParam param;

    public EventsFetcher(Activity activity, EventsFetcherCallBack callback) {
        this.activity = activity;
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
            url = API_ENDPOINT_DATE.replace("CITY", param.city.toString().toLowerCase())
                    .replace("DATE", Utils.getDateString(Utils.getDate(param.day)));
        } else {
            try {
                url = API_ENDPOINT_QUERY.replace("CITY", param.city.toString().toLowerCase())
                        .replace("QUERY", URLEncoder.encode(param.query, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                Log.w(LOG_TAG, "Invalid Query", e);
                return null;
            }
        }
        Log.d(LOG_TAG, "fetching: " + url);

        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            try {
                StringBuilder jsonBuffer = new StringBuilder();
                for (String json : Utils.readStream(urlConnection.getInputStream())) {
                    jsonBuffer.append(json);
                }
                return new Builder(param.query.isEmpty()).addAllEvent(
                        Event.parseUpcomingEvents(param.city, jsonBuffer.toString())).build();
            } finally {
                urlConnection.disconnect();
            }
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
