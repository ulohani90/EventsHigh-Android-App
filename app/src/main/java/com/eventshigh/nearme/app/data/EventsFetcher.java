package com.eventshigh.nearme.app.data;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.Utils;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventsFetcher extends AsyncTask<EventFetcherParam, Void, List<Event>> {

    private static final String LOG_TAG = EventsFetcher.class.getSimpleName();
    private static final String API_ENDPOINT =
            "http://api.eventshigh.com/date/CITY/DATE?sortby=popularity&limit=50";

    private static final Map<String, String> cityNameMapping = new HashMap<String, String>();
    static {
        cityNameMapping.put("bengaluru", "bangalore");
    }

    public static interface EventsFetcherCallBack {
        public void OnEventsAvailable(List<Event> events);
    }

    private final Context context;
    private final EventsFetcherCallBack callback;
    private ProgressDialog pDialog;

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
    protected List<Event> doInBackground(EventFetcherParam... params) {
        EventFetcherParam param = params[0];

        Geocoder gcd = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(param.location.latitude, param.location.longitude, 1);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to locate city from location!", e);
        }

        if (addresses == null || addresses.size() == 0) {
            // Failed
            return  null;
        }

        String city = addresses.get(0).getLocality().toLowerCase();
        if (cityNameMapping.containsKey(city)) {
            city = cityNameMapping.get(city);
        }

        String url = API_ENDPOINT.replace("CITY", city)
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
                return Event.fromJSON(buffer.toString());
            } finally {
                urlConnection.disconnect();
            }
        } catch (ParseException e) {
            Log.e(LOG_TAG, "Failed to fetch events, malformed DATE!", e);
            return null;
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Failed to fetch events, malformed JSON!", e);
            return null;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to fetch events!", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(@Nullable List<Event> result) {
        super.onPostExecute(result);
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }

        if (result == null) {
            // Failed. Show toast and return empty list.
            Toast.makeText(context, R.string.failed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.isEmpty()) {
            // Failed. Show toast and return empty list.
            Toast.makeText(context, R.string.no_event, Toast.LENGTH_SHORT).show();
            return;
        }

        callback.OnEventsAvailable(result);
    }
}
