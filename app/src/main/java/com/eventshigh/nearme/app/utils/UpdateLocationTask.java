package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * {@link android.os.AsyncTask} which can be used to update the location in a
 * {@link android.widget.TextView} based on lat-long co-ordinated.
*/
public class UpdateLocationTask extends AsyncTask<LatLng, Void, String> {

    // Constants
    private static final String LOG_TAG = UpdateLocationTask.class.getSimpleName();
    private static final Pattern INVALID_LOCALITY_PATTERN = Pattern.compile("[^a-zA-Z]+[a-zA-Z]?");

    private final Context mContext;
    private final TextView mLocalityView;

    public UpdateLocationTask(Context context, TextView localityView) {
        this.mContext = context;
        this.mLocalityView = localityView;
    }

    @Override
    protected String doInBackground(LatLng... params) {
        String locality = null;
        List<Address> addresses = null;
        try {
            addresses = new Geocoder(mContext)
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
            mLocalityView.setText(locality);
        }
    }

    private String checkLocality(String locality) {
        if (INVALID_LOCALITY_PATTERN.matcher(locality).matches()) {
            return null;
        }
        return locality;
    }
}
