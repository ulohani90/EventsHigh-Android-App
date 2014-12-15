package com.eventshigh.nearme.app.data;

import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Represents the City as supported by EventsHigh.
 */
public enum City {
    BANGALORE(12.784305f, 77.352505f, 13.178402f, 77.840023f, "GMT+0530", "IN"),
    CHENNAI(12.845130f, 80.036949f, 13.259850f, 80.358986f, "GMT+0530", "IN"),
    DELHI(28.370917f, 76.803156f, 28.882014f, 77.432123f, "GMT+0530", "IN"),
    HYDERABAD(17.221264f, 78.208575f, 17.593416f, 78.696093f, "GMT+0530", "IN"),
    KOLKATA(22.363021f, 88.076763f, 23.018055f, 88.590374f, "GMT+0530", "IN"),
    MUMBAI(18.863098f, 72.750398f, 19.295282f, 73.071748f, "GMT+0530", "IN"),
    PUNE(18.419274f, 73.731990f, 18.645184f, 74.008021f, "GMT+0530", "IN"),
    SINGAPORE(1.154025f, 103.581051f, 1.479752f, 104.027027f, "GMT+8", "SG"),
    JAKARTA(-6.380236f, 106.643094f, -6.066924f, 106.987103f, "GMT+7", "ID");

    public final LatLngBounds cityBounds;
    public final String timeZone;
    public final String countryCode;

    private City(float lat1, float lng1, float lat2, float lng2, String timeZone, String countryCode) {
        cityBounds = new LatLngBounds(new LatLng(lat1, lng1), new LatLng(lat2, lng2));
        this.timeZone = timeZone;
        this.countryCode = countryCode;
    }

    public static City getCity(@Nullable LatLng point) {
        if (point == null) {
            return null;
        }

        for (City city : City.values()) {
            if (city.cityBounds.contains(point)) {
                return city;
            }
        }

        return null;
    }
}
