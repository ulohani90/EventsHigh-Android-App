package com.eventshigh.nearme.app.task;

import android.app.ActionBar;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * {@link android.os.AsyncTask} which can be used to fetch the locality from LatLng and then
 * update the {@link android.app.ActionBar} subtitle with locality.
*/
public class ShowLocalityTask extends AsyncTask<LatLng, Void, String> {
    // Constants
    private static final String LOG_TAG = ShowLocalityTask.class.getSimpleName();
    private static final Pattern INVALID_LOCALITY_PATTERN = Pattern.compile("[^a-zA-Z]+[a-zA-Z]?");

    private final Context context;
    private final ActionBar actionBar;

    public ShowLocalityTask(Context context, ActionBar actionBar) {
        this.context = context;
        this.actionBar = actionBar;
    }

    @Override
    protected String doInBackground(LatLng... params) {
        String locality = null;
        List<Address> addresses = null;
        try {
            addresses = new Geocoder(context)
                    .getFromLocation(params[0].latitude, params[0].longitude, 1);
        } catch (IOException e) {
            Log.w(LOG_TAG, "failed to get address", e);
        }

        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);

            locality = address.getSubLocality();
            if (locality == null) {
                locality = checkLocality(address.getFeatureName());
            }

            for (int i = 0; locality == null && i < address.getMaxAddressLineIndex(); i++) {
                locality = checkLocality(address.getAddressLine(i));
            }

            String city = address.getLocality();
            if (city != null) {
                locality = (locality == null ? "" : locality + ", ") + city;
            }
        }
        return locality;
    }

    @Override
    protected void onPostExecute(@Nullable String locality) {
        if (locality != null && !locality.isEmpty()) {
            actionBar.setSubtitle(locality);
        }
    }

    private String checkLocality(String locality) {
        if (INVALID_LOCALITY_PATTERN.matcher(locality).matches()) {
            return null;
        }
        return locality;
    }
}
