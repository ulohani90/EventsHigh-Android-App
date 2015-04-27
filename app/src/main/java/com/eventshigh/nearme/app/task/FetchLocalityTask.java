package com.eventshigh.nearme.app.task;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;

import com.eventshigh.nearme.app.utils.LocationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * {@link android.os.AsyncTask} which can be used to fetch the locality from LatLng and then
 * calls {@link FetchLocalityTask.Listener#onLocationUpdated(String)}.
*/
public class FetchLocalityTask extends AsyncTask<LatLng, Void, String> {
    public interface Listener {
        void onLocationUpdated(String locality);
    }

    // Constants
    private static final String LOG_TAG = FetchLocalityTask.class.getSimpleName();
    private static final Pattern INVALID_LOCALITY_PATTERN = Pattern.compile("[^a-zA-Z]+[a-zA-Z]?");

    private static final LruCache<String, String> LAT_LNG_TO_LOCALITY = new LruCache<>(100);

    private final Context context;
    private final Listener listener;

    public FetchLocalityTask(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(LatLng... params) {
        // Check for Cache if entry is there.
        String key = LocationUtils.toString(params[0]);
        String locality = LAT_LNG_TO_LOCALITY.get(key);
        if (locality != null) {
            return locality;
        }

        // Use GeoCoder for lat lng to locality.
        List<Address> addresses = null;
        try {
            addresses = new Geocoder(context)
                    .getFromLocation(params[0].latitude, params[0].longitude, 1);
        } catch (IOException|SecurityException e) {
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

        if (locality != null && !locality.isEmpty()) {
            LAT_LNG_TO_LOCALITY.put(key, locality);
        }
        return locality;
    }

    @Override
    protected void onPostExecute(@Nullable String locality) {
        listener.onLocationUpdated(locality);
    }

    private String checkLocality(String locality) {
        if (INVALID_LOCALITY_PATTERN.matcher(locality).matches()) {
            return null;
        }
        return locality;
    }


}
