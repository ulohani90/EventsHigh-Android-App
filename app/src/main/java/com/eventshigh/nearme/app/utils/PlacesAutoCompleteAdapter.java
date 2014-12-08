package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
* See https://developers.google.com/places/training/autocomplete-android.
*/
public class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
    private static final String LOG_TAG = PlacesAutoCompleteAdapter.class.getSimpleName();
    private static final String PLACES_API_BASE =
            "https://maps.googleapis.com/maps/api/place/autocomplete/json?key=AIzaSyB1O1j8ffmWWwxl8nZ1VenkjzBhJagXzBA";

    private ArrayList<String> resultList;
    private final String countryCode;

    public PlacesAutoCompleteAdapter(Context context, int textViewResourceId, String countryCode) {
        super(context, textViewResourceId);
        this.countryCode = countryCode;
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public String getItem(int index) {
        return resultList.get(index);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    // Retrieve the auto complete results.
                    resultList = autoComplete(constraint.toString(), countryCode);

                    // Assign the data to the FilterResults
                    filterResults.values = resultList;
                    filterResults.count = resultList.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }};
    }

    public static ArrayList<String> autoComplete(String input, String countryCode) {
        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE);
            if (countryCode != null) {
                sb.append("&components=country:").append(countryCode);
            }
            URL url = new URL(sb.append("&types=geocode")
                    .append("&input=").append(URLEncoder.encode(input, "utf8"))
                    .toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return null;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        ArrayList<String> resultList = null;
        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList<>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }

}
