package com.eventshigh.nearme.app.task;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.ui.LocationPickerDialog.OnLocationSelection;
import com.eventshigh.nearme.app.utils.GAHelper;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

/**
* Created by paragsarda on 02/01/15.
*/
public class LatLngFetcher extends AsyncTask<String, Void, Pair<String, LatLng>> {
    private static final String LOG_TAG = LatLngFetcher.class.getSimpleName();

    private final Context context;
    private final OnLocationSelection onLocationSelection;

    public LatLngFetcher(Context context, OnLocationSelection onLocationSelection) {
        this.context = context;
        this.onLocationSelection = onLocationSelection;
    }

    @Override
    protected Pair<String, LatLng> doInBackground(String... params) {
        try {
            Geocoder geocoder = new Geocoder(context);
            List<Address> addresses = geocoder.getFromLocationName(params[0], 1);
            if (addresses.isEmpty() ||
                    !addresses.get(0).hasLatitude() ||
                    !addresses.get(0).hasLatitude()) {
                throw new IOException("Geocoding failed, no address returned.");
            }

            return Pair.create(params[0],
                    new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude()));
        } catch (IOException e) {
            Log.w(LOG_TAG, "failed to fetch the address", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(@Nullable Pair<String, LatLng> locality) {
        if (locality != null) {
            onLocationSelection.onLocationSelection(locality.first, locality.second);
        } else {
            GAHelper.getInstance(context).reportActionToAnalytics(LOG_TAG, "failedGeoCoding", "", 0);
            Toast.makeText(context, R.string.failed_locality, Toast.LENGTH_SHORT).show();
        }
    }
}
