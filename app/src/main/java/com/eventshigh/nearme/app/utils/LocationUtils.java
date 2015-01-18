package com.eventshigh.nearme.app.utils;

import android.graphics.Point;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

/**
 * Helper methods for managing location and latlng.
 */
public class LocationUtils {
    public static LatLng locationToLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public static float distanceInMeters(LatLng loc1, LatLng loc2) {
        float[] distance = new float[1];
        Location.distanceBetween(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude, distance);
        return distance[0];
    }

    public static float getDistanceSQ(Point p1, Point p2) {
        return (p1.x - p2.x) * (p1.x - p2.x) +  (p1.y - p2.y) * (p1.y - p2.y);
    }
}
