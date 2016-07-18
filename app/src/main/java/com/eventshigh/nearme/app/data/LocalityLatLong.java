package com.eventshigh.nearme.app.data;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by umesh on 13/06/16.
 */
public class LocalityLatLong {

    private String name;

    private LatLng latLng;

    public String getName() {
        return name;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public LocalityLatLong(String name, LatLng latLng) {
        this.name = name;
        this.latLng = latLng;
    }

    public static LocalityLatLong getLocality(String locality) {
        try {
            if (locality.length() > 0) {
                String[] localityArray = locality.split(",");
                return new LocalityLatLong(localityArray[0], new LatLng(Double.parseDouble(localityArray[1]), Double.parseDouble(localityArray[2])));
            }
        } catch (NumberFormatException e) {
            Crashlytics.logException(e);
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (name.equalsIgnoreCase(((LocalityLatLong) obj).getName()) && latLng.equals(((LocalityLatLong) obj).getLatLng()))
            return true;
        return false;
    }
}
