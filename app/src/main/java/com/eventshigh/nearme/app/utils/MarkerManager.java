package com.eventshigh.nearme.app.utils;

import android.graphics.Point;
import android.util.Pair;

import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the markers on {@link com.google.android.gms.maps.GoogleMap} for events.
 * This object is responsible for creating marker objects and managing the visibility
 * of the markers.
 */
public class MarkerManager {

    // ***********************
    // CONSTANTS
    // ***********************

    // To avoid the map cluttering, we might show few event markers as DOT instead of
    // as category icon. We show event marker as dot, only if total number of events
    // shown is greater than NUM_MIN_EVENTS and if event popularity score is less than
    // MIN_POPULARITY.
    private static final int NUM_MIN_EVENTS = 15;
    private static final int MIN_POPULARITY = 20;

    // To avoid cluttering, we do not show marker for event if it happens to be within
    // small distance from other event. This parameter controls that distance as measured
    // in screen units.
    private static final int MIN_MARKER_DISTANCE_SQ = 2000;


    // ***********************
    // MEMBERS
    // ***********************

    // Information associated with Marker.
    private static class MarkerInfo {
        public final Event event;
        public final boolean canShowAsDot;
        public boolean shownAsDot;

        public MarkerInfo(Event event) {
            this.event = event;
            canShowAsDot = event.getPopularityScore() < MIN_POPULARITY;
            shownAsDot = canShowAsDot;
        }
    }

    // Markers currently created on Maps. Each marker represents one event.
    // Note that all markers may not be visible to user.
    private final Map<Marker, MarkerInfo> markers = new HashMap<Marker, MarkerInfo>();

    // We show the InfoWindow explicitely for first time only.
    boolean showInfoWindow = true;


    // ***********************
    // Public methods which are used to manage markers.
    // ***********************

    public Event getEvent(Marker marker) {
        return markers.get(marker).event;
    }

    public void setEvents(GoogleMap map, List<Event> events) {
        map.clear();
        markers.clear();

        for(Event event : events) {
            MarkerInfo markerInfo = new MarkerInfo(event);
            Marker marker = map.addMarker(
                    new MarkerOptions()
                            .position(new LatLng(event.location.latitude, event.location.longitude))
                            .visible(false)
                            .icon(markerInfo.canShowAsDot ?
                                    EventCategory.circleIcon() :
                                    event.category.icon())
            );

            markers.put(marker, markerInfo);
        }

        updateListingForProjection(map.getProjection());
    }

    // Updates the listing for current maps projection. This method decides which markers should
    // be visible and which one should not be visible. Also few markers are highlighted to
    // give relevance information.
    public void updateListingForProjection(Projection projection) {
        // First find the markers which are withing visible region bound. All other
        // markers are marked invisible.
        LatLngBounds bounds = projection.getVisibleRegion().latLngBounds;
        List<Marker> markersInProjection = new ArrayList<Marker>();
        for (Marker marker : markers.keySet()) {
            if (bounds.contains(marker.getPosition())) {
                markersInProjection.add(marker);
            } else {
                marker.setVisible(false);
            }
        }

        // We now sort the markers within visible region based on the relevance or
        // popularity score.
        Collections.sort(markersInProjection, new Comparator<Marker>() {
            @Override
            public int compare(Marker lhs, Marker rhs) {
                if (lhs.isInfoWindowShown()) {
                    return -1;
                }
                if (rhs.isInfoWindowShown()) {
                    return 1;
                }

                return Integer.valueOf(markers.get(rhs).event.getPopularityScore()).compareTo(
                        markers.get(lhs).event.getPopularityScore());
            }
        });

        // We now show as much point as possible so that no two markers are very close.
        // Few first markers (high popularity score) are highlighted.
        List<Pair<Point, Boolean>> shownPoints =
                new ArrayList<Pair<Point, Boolean>>(markersInProjection.size());
        for (Marker marker : markersInProjection) {
            Event event = markers.get(marker).event;
            Point point = projection.toScreenLocation(event.location);

            // Is this marker too close to other marker? if yes then we do not show it.
            boolean toClose = false;
            for (Pair<Point, Boolean> shownPoint : shownPoints) {
                // if we have shown the marker as dot, we can reduce the min distance constrain.
                int minDistSq = shownPoint.second ?
                        MIN_MARKER_DISTANCE_SQ :
                        MIN_MARKER_DISTANCE_SQ / 4;
                if (Utils.getDistanceSQ(shownPoint.first, point) < minDistSq) {
                    toClose = true;
                    break;
                }
            }

            marker.setVisible(!toClose);
            if (!toClose) {
                MarkerInfo markerInfo = markers.get(marker);
                if (!marker.isInfoWindowShown() && markerInfo.canShowAsDot) {
                    boolean shouldShowAtDot = markersInProjection.size() > NUM_MIN_EVENTS;
                    if (shouldShowAtDot != markerInfo.shownAsDot) {
                        marker.setIcon(shouldShowAtDot ?
                                EventCategory.circleIcon() :
                                event.category.icon());
                        markerInfo.shownAsDot = shouldShowAtDot;
                    }
                }
                shownPoints.add(Pair.create(point, markerInfo.shownAsDot));
            }
        }

        // Show the info card for highest popular event.
        if (showInfoWindow && !markersInProjection.isEmpty()) {
            markersInProjection.get(0).showInfoWindow();
            showInfoWindow = false;
        }
    }
}
