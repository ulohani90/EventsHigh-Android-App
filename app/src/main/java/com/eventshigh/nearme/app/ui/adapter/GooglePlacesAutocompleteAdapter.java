package com.eventshigh.nearme.app.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLngBounds;

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
 * @author shubham
 * @since 14/6/16.
 */

public class GooglePlacesAutocompleteAdapter extends BaseAdapter implements Filterable {
    private static final String LOG_TAG = "Places Autocomplete";
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";

    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";
    private static final String API_KEY = "AIzaSyDhkyBFReMAFi4fljsEuxZPGlglx5DACV4";
    private static final String BOUNDS = "bounds";
    private ArrayList<ArrayList<String>> resultList;
    private Context context = null;
    LatLngBounds bounds;

    String cityName;

    public GooglePlacesAutocompleteAdapter(Context context, String cityName, LatLngBounds bounds) {
        this.bounds = bounds;
        this.context = context;
        this.cityName = cityName;
    }


    @Override
    public int getCount() {
        if (resultList != null)
            return resultList.size();
        else
            return 0;
    }

    @Override
    public Object getItem(int index) {
        return resultList.get(index);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.places_list_item_layout, parent, false);
            ViewHolder holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        ViewHolder holder = (ViewHolder) convertView.getTag();
        holder.placeName.setText(resultList.get(position).get(0));
        holder.placeDesc.setText(getDescString(resultList.get(position)));

        return convertView;
    }

    public String getDescString(ArrayList<String> terms) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < terms.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(terms.get(i));
        }
        return sb.toString();
    }

    public ArrayList<ArrayList<String>> autocomplete(String input) {
        ArrayList<ArrayList<String>> resultList = null;


        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&components=country:in");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));
            sb.append("&bounds=" + bounds.toString());

            URL url = new URL(sb.toString());
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
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            Log.d(LOG_TAG, jsonResults.toString());
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");


            // Extract the Place descriptions from the results
            resultList = new ArrayList<>();

            for (int i = 0; i < predsJsonArray.length(); i++) {
                ArrayList<String> terms = new ArrayList<>();
                JSONArray termsArray = predsJsonArray.getJSONObject(i).getJSONArray("terms");
                if (termsArray != null) {
                    for (int j = 0; j < termsArray.length(); j++) {
                        terms.add(termsArray.getJSONObject(j).getString("value"));
                    }

                }
                if (terms.size() > 0)
                    resultList.add(terms);

            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }


    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                ArrayList<ArrayList<String>> filteredData = new ArrayList<>();
                if (constraint != null) {
                    // Retrieve the autocomplete results.
                    resultList = autocomplete(constraint.toString());
                    for (int i = 0; i < resultList.size(); i++) {
                        ArrayList<String> terms = resultList.get(i);
                        for (int j = 0; j < terms.size(); j++) {
                            if (terms.get(j).equalsIgnoreCase(cityName)) {
                                filteredData.add(terms);
                                break;
                            }
                        }
                    }
                    // Assign the data to the FilterResults
                    resultList = filteredData;
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
            }
        };
        return filter;
    }

    public class ViewHolder {

        TextView placeName, placeDesc;

        public ViewHolder(View view) {
            this.placeName = (TextView) view.findViewById(R.id.place_name);
            this.placeDesc = (TextView) view.findViewById(R.id.place_desc);
        }
    }


}