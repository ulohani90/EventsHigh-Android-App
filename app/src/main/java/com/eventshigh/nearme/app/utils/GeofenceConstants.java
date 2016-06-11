package com.eventshigh.nearme.app.utils;

/**
 * @author shubham
 * @since 9/6/16.
 */


import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

/**
 * Constants used in this sample.
 */
public final class GeofenceConstants{

    private GeofenceConstants() {
    }

    public static final String PACKAGE_NAME = "com.google.android.gms.location.Geofence";

    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES_VENUES_EH";

    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    /**
     * Used to set an expiration time for a geofence. After this amount of time Location Services
     * stops tracking the geofence.
     */
    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;

    /**
     * For this sample, geofences expire after twelve hours.
     */
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    public static final float GEOFENCE_RADIUS_IN_METERS = 150; // 1 mile, 1.6 km

    /**
     * Map for storing information about airports in the San Francisco bay area.
     */
    public static final HashMap<String, LatLng> BAY_AREA_LANDMARKS = new HashMap<String, LatLng>();
    static {
        // San Francisco International Airport.
        BAY_AREA_LANDMARKS.put("Eventshigh Office", new LatLng(12.956269, 77.637704));

        BAY_AREA_LANDMARKS.put("ranga shankara", new LatLng(12.911500, 77.587026));
        BAY_AREA_LANDMARKS.put("karnataka chitrakala parishath", new LatLng(12.989059, 77.580605));
        BAY_AREA_LANDMARKS.put("bangalore university", new LatLng(12.942687, 77.509154));
        BAY_AREA_LANDMARKS.put("m chinnaswamy stadium", new LatLng(12.978860, 77.599553));
        BAY_AREA_LANDMARKS.put("high ultra lounge", new LatLng(13.012089, 77.556100));
        BAY_AREA_LANDMARKS.put("cubbon park", new LatLng(12.973930, 77.589873));
        BAY_AREA_LANDMARKS.put("jakkur airfield", new LatLng(13.079361, 77.607580));
        BAY_AREA_LANDMARKS.put("lalbagh", new LatLng(12.950780, 77.584767));
        BAY_AREA_LANDMARKS.put("pebble", new LatLng(13.005070, 77.584500));
        BAY_AREA_LANDMARKS.put("orion mall", new LatLng(13.010919, 77.554963));

//        BAY_AREA_LANDMARKS.put("chamrajpet", new LatLng(0, 0));//this is locality
        BAY_AREA_LANDMARKS.put("ktpo convention center", new LatLng(12.979907, 77.720549));
        BAY_AREA_LANDMARKS.put("ub city", new LatLng(12.971502, 77.596366));
        BAY_AREA_LANDMARKS.put("the humming tree", new LatLng(12.970201, 77.639242));
        BAY_AREA_LANDMARKS.put("palace grounds bangalore", new LatLng(13.010275, 77.583968));//this is small point
        BAY_AREA_LANDMARKS.put("kanteerava stadium", new LatLng(12.969598, 77.593136));
        BAY_AREA_LANDMARKS.put("rangoli the metro art center", new LatLng(12.976268, 77.603812));
        BAY_AREA_LANDMARKS.put("phoenix marketcity", new LatLng(12.997101, 77.696382));
        BAY_AREA_LANDMARKS.put("chowdiah memorial hall", new LatLng(13.006561, 77.575479));
        BAY_AREA_LANDMARKS.put("that comedy club", new LatLng(12.974669, 77.607107));

        BAY_AREA_LANDMARKS.put("indigo live music bar", new LatLng(12.932837, 77.614109));
        BAY_AREA_LANDMARKS.put("atta galatta", new LatLng(12.934133, 77.616985));
        BAY_AREA_LANDMARKS.put("loft 38", new LatLng(12.971166, 77.640897));
        BAY_AREA_LANDMARKS.put("garuda mall", new LatLng(12.970008, 77.609794));
        BAY_AREA_LANDMARKS.put("jain farms", new LatLng(12.939873, 77.571255));
        BAY_AREA_LANDMARKS.put("i-bar", new LatLng(12.974026, 77.619342));
        BAY_AREA_LANDMARKS.put("sunny's", new LatLng(12.971896, 77.598608));
        BAY_AREA_LANDMARKS.put("the tao terraces", new LatLng(12.973667, 77.620392));
        BAY_AREA_LANDMARKS.put("hard rock cafe bengaluru", new LatLng(12.976044, 77.601463));
        BAY_AREA_LANDMARKS.put("forum mall", new LatLng(12.935432, 77.610852));

        BAY_AREA_LANDMARKS.put("save sadan malleswaram", new LatLng(13.004456, 77.570187));
        BAY_AREA_LANDMARKS.put("get beyond limits", new LatLng(12.914426, 77.599814));
        BAY_AREA_LANDMARKS.put("om made cafe", new LatLng(12.933782, 77.621428));
        BAY_AREA_LANDMARKS.put("jagriti theatre", new LatLng(12.957577, 77.740759));
        BAY_AREA_LANDMARKS.put("the sugar factory", new LatLng(12.990195, 77.586447));
//        BAY_AREA_LANDMARKS.put("sarjapur road", new LatLng(0,0));//this is a road
        BAY_AREA_LANDMARKS.put("bluefrog bangalore", new LatLng(12.974419, 77.606731));
//        BAY_AREA_LANDMARKS.put("aqua", new LatLng(12.973787, 77.619657));//this is small point
        BAY_AREA_LANDMARKS.put("153 biere street", new LatLng(12.962706, 77.750788));
        BAY_AREA_LANDMARKS.put("the biere club", new LatLng(12.970988, 77.597573));

        BAY_AREA_LANDMARKS.put("ice - vivanta by taj", new LatLng(12.973287, 77.619711));
        BAY_AREA_LANDMARKS.put("opus", new LatLng(12.973287, 77.619711));
//        BAY_AREA_LANDMARKS.put("bhive workspace", new LatLng(0,0));//there are two bhive
        BAY_AREA_LANDMARKS.put("jus' trufs", new LatLng(13.072468, 77.603746));
        BAY_AREA_LANDMARKS.put("lahe lahe", new LatLng(12.966615, 77.648331));
//        BAY_AREA_LANDMARKS.put("aloft hotel", new LatLng(0,0));//there are two alof

    }
}