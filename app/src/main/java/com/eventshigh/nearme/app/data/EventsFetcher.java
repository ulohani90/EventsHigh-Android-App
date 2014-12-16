package com.eventshigh.nearme.app.data;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsCollection.Builder;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * An {@link AsyncTask} which is used to fetch the Events. Once events are available,
 * the callback is called on UI thread. The Caller can then use Callback to refresh the events.
 *
 * Before using EventsFetcher, its recommended to setup {@link android.net.http.HttpResponseCache}
 * for better performance.
 */
public class EventsFetcher extends AsyncTask<EventFetcherParam, Void, EventsCollection> {

    private static final String LOG_TAG = EventsFetcher.class.getSimpleName();
    private static final String API_ENDPOINT =
            "http://apiserver.eventshigh.com:8888/api/date/CITY/DATE?sortby=popularity&limit=200&mobile=1";

    public static interface EventsFetcherCallBack {
        public void OnEventsAvailable(EventFetcherParam param, EventsCollection events);
    }

    private final Context context;
    private final EventsFetcherCallBack callback;
    private ProgressDialog pDialog;
    private EventFetcherParam param;

    public EventsFetcher(Context context, EventsFetcherCallBack callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pDialog = new ProgressDialog(context);
        pDialog.setMessage(context.getString(R.string.loading));
        pDialog.setCancelable(false);
        pDialog.show();
    }

    @Override
    protected EventsCollection doInBackground(EventFetcherParam... params) {
        param = params[0];

        String url = API_ENDPOINT.replace("CITY", param.city.toString().toLowerCase())
                .replace("DATE", Utils.getDateString(Utils.getDate(param.day)));
        Log.d(LOG_TAG, "fetching: " + url);

        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                return new Builder().addAllEvent(
                        Event.parseUpcomingEvents(param.city, buffer.toString())).build();
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
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }

        if (result == null || result.getTags().isEmpty()) {
            // Failed. Show toast and return empty list.
            Toast.makeText(context, R.string.failed_load, Toast.LENGTH_SHORT).show();
            return;
        }

        callback.OnEventsAvailable(param, result);
    }
}
