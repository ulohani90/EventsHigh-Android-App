package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.google.android.gms.maps.model.LatLng;

/**
 * Helper methods for managing location and latlng.
 */
public class LocationUtils {
    public static LatLng locationToLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public static float distanceInKM(LatLng loc1, LatLng loc2) {
        return distanceInMeters(loc1, loc2) / 1000;
    }

    public static float distanceInMeters(LatLng loc1, LatLng loc2) {
        float[] distance = new float[1];
        Location.distanceBetween(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude, distance);
        return distance[0];
    }

    public static float getDistanceSQ(Point p1, Point p2) {
        return (p1.x - p2.x) * (p1.x - p2.x) +  (p1.y - p2.y) * (p1.y - p2.y);
    }

    private static final String LAT_LNG_STR_FORMAT = "%.2f-%.2f";
    public static String toString(LatLng loc) {
        return String.format(LAT_LNG_STR_FORMAT, loc.latitude, loc.longitude);
    }


    private static final int RADIAL_DISTANCE_TO_TIME_MULTIPLIER = 5;
    public static @Nullable String getTravelTime(Context context, @Nullable LatLng userLocation,
                                       @Nullable LatLng destination) {
        // If user location is very close to city center, we assume userLocation as null.
        // This generally happens when we do not have user location and we take city center as
        // user location.
        City userCity = City.getCity(userLocation);
        if (userLocation == null || destination == null || userCity == null ||
            LocationUtils.distanceInMeters(userLocation, userCity.cityBounds.getCenter()) < 20) {
            return null;
        }

        float radialDistance = LocationUtils.distanceInKM(userLocation, destination);
        int travelTime = (int)(radialDistance * RADIAL_DISTANCE_TO_TIME_MULTIPLIER);
        return String.format(context.getResources().getString(R.string.event_distance_and_time),
                travelTime);
    }
}
