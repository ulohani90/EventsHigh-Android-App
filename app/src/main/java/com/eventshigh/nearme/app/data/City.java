package com.eventshigh.nearme.app.data;

import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Represents the City as supported by EventsHigh.
 */
public enum City {
    BANGALORE(12.784305f, 77.352505f, 13.178402f, 77.840023f, "GMT+0530", "IN"),
    DELHI(28.370917f, 76.803156f, 28.882014f, 77.432123f, "GMT+0530", "IN"),
    MUMBAI(18.863098f, 72.750398f, 19.295282f, 73.071748f, "GMT+0530", "IN");

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

    public static City getCity(String cityName) {
        String cityNameInUpperCase = cityName.toUpperCase();
        for (City city : City.values()) {
            if (city.name().equals(cityNameInUpperCase)) {
                return city;
            }
        }

        return null;
    }

    public static String[] getValuesAsString() {
        City[] cities = values();
        String[] cityNames = new String[cities.length];
        for (int i = 0; i < cities.length; i++) {
            cityNames[i] = cities[i].name();
        }

        return cityNames;
    }
}
