package com.eventshigh.nearme.app.data;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Represents the City as supported by EventsHigh.
 */
public enum City {
    BANGALORE(12.784305f, 77.352505f, 13.178402f, 77.840023f),
    CHENNAI(12.845130f, 80.036949f, 13.259850f, 80.358986f),
    DELHI(28.370917f, 76.803156f, 28.882014f, 77.432123f),
    HYDERABAD(17.221264f, 78.208575f, 17.593416f, 78.696093f),
    KOLKATA(22.363021f, 88.076763f, 23.018055f, 88.590374f),
    MUMBAI(18.863098f, 72.750398f, 19.295282f, 73.071748f),
    PUNE(18.419274f, 73.731990f, 18.645184f, 74.008021f);

    public final LatLngBounds cityBounds;

    private City (float lat1, float lng1, float lat2, float lng2) {
        cityBounds = new LatLngBounds(new LatLng(lat1, lng1), new LatLng(lat2, lng2));
    }

    public static City getCity(LatLng point) {
        for (City city : City.values()) {
            if (city.cityBounds.contains(point)) {
                return city;
            }
        }

        return null;
    }
}
