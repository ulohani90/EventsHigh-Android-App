package com.eventshigh.nearme.app.ui;

import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Pair;

import com.eventshigh.nearme.app.activity.EventsMapsActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Manages the markers on {@link com.google.android.gms.maps.GoogleMap} for events.
 * This object is responsible for creating marker objects and managing the visibility
 * of the markers.
 */
public class MapMarkerManager {

    // ***********************
    // CONSTANTS
    // ***********************

    // To avoid the map cluttering, we might show few event markers as DOT instead of
    // as category icon. We show event marker as dot, only if total number of events
    // shown is greater than NUM_MIN_EVENTS and if event popularity score is less than
    // MIN_POPULARITY.
    private static final int NUM_MIN_EVENTS = 25;
    private static final int MIN_POPULARITY = 20;

    // To avoid cluttering, we do not show marker for event if it happens to be within
    // small distance from other event. This parameter controls that distance as measured
    // in screen units.
    private static final int MIN_MARKER_DISTANCE_SQ = 6000;

    // When events are loaded, we zoom out if there are not enough events shown on map.
    private static final int MIN_EVENTS_TO_SHOW = 10;
    // Because screens are not perfect circle, we do approximation here to diagonal distance
    // needed is 2.79f times the event distance. Note: 2.79 ~= 1.5 * (sq root of 5), where
    // (sq root of 5) is diagonal distance for 9:16 wide screens.
    private static final float DIAGONAL_DISTANCE_MULTIPLIER = 3.35f;

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
            canShowAsDot = event.uberScore < MIN_POPULARITY;
            shownAsDot = canShowAsDot;
        }
    }

    // Markers currently created on Maps. Each marker represents one event.
    // Note that all markers may not be visible to user.
    private final LinkedHashMap<Marker, MarkerInfo> markers = new LinkedHashMap<>();

    // ***********************
    // Public methods which are used to manage markers.
    // ***********************

    public Event getEvent(Marker marker) {

        return markers != null ? markers.get(marker).event : null;
    }

    public void setEvents(GoogleMap map, List<Event> events) {
        map.clear();
        markers.clear();

        for (Event event : events) {
            if (event.location == null) {
                continue;
            }

            MarkerInfo markerInfo = new MarkerInfo(event);
            Marker marker = map.addMarker(
                    new MarkerOptions()
                            .position(new LatLng(event.location.latitude, event.location.longitude))
                            .title(Utils.shortenIfNeeded(event.title))
                            .visible(false)
                            .icon(event.ehRecommended ? event.category.recommendationIcon()
                                    : markerInfo.canShowAsDot ? EventCategory.circleIcon()
                                    : event.category.icon())
            );
            markers.put(marker, markerInfo);
        }

        updateListingForProjection(map.getProjection());

        // If the user has zoomed in too much, zoom out a bit.
        new MapZoomChecker(map, markers.keySet()).execute();
    }

    // Gets the next marker, which are ordered based on user location and event popularity.
    public Marker getNextMarker(Marker marker) {
        Marker first = null;
        boolean found = false;
        for (Marker m : markers.keySet()) {
            if (first == null) {
                first = m;
            }
            if (found) {
                return m;
            }
            if (m.equals(marker)) {
                found = true;
            }
        }

        return first;
    }

    public Marker getPrevMarker(Marker marker) {
        Marker last = null;
        for (Marker m : markers.keySet()) {
            if (m.equals(marker)) {
                if (last != null) {
                    return last;
                }
            }
            last = m;
        }

        return last;
    }

    // Updates the listing for current maps projection. This method decides which markers should
    // be visible and which one should not be visible. Also few markers are highlighted to
    // give relevance information.
    public boolean updateListingForProjection(Projection projection) {
        // First find the markers which are within visible region bound. All other
        // markers are marked invisible.
        LatLngBounds bounds = projection.getVisibleRegion().latLngBounds;
        List<Marker> markersInProjection = new ArrayList<>();
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

                return Float.valueOf(markers.get(rhs).event.uberScore).compareTo(
                        markers.get(lhs).event.uberScore);
            }
        });

        // We now show as much point as possible so that no two markers are very close.
        // Few first markers (high popularity score) are highlighted.
        List<Pair<Point, Boolean>> shownPoints =
                new ArrayList<>(markersInProjection.size());
        for (Marker marker : markersInProjection) {
            Event event = markers.get(marker).event;
            Point point = projection.toScreenLocation(event.location);

            // Is this marker too close to other marker? if yes then we do not show it.
            boolean toClose = false;
            for (Pair<Point, Boolean> shownPoint : shownPoints) {
                // if we have shown the marker as dot, we can reduce the min distance constraint.
                int minDistSq = shownPoint.second ?
                        MIN_MARKER_DISTANCE_SQ :
                        MIN_MARKER_DISTANCE_SQ / 4;
                if (LocationUtils.getDistanceSQ(shownPoint.first, point) < minDistSq) {
                    toClose = true;
                    break;
                }
            }

            if (!toClose) {
                MarkerInfo markerInfo = markers.get(marker);
                if (!marker.isInfoWindowShown() && markerInfo.canShowAsDot) {
                    boolean shouldShowAtDot = shownPoints.size() > NUM_MIN_EVENTS;
                    if (shouldShowAtDot != markerInfo.shownAsDot) {
                        marker.setIcon(shouldShowAtDot ?
                                EventCategory.circleIcon() :
                                event.category.icon());
                        markerInfo.shownAsDot = shouldShowAtDot;
                    }
                }
                shownPoints.add(Pair.create(point, markerInfo.shownAsDot));
            }

            marker.setVisible(!toClose);
        }

        return !markersInProjection.isEmpty() && markersInProjection.get(0).isInfoWindowShown();
    }

    private static class MapZoomChecker extends AsyncTask<Void, Void, Float> {
        private final List<LatLng> markerPositions;
        private final GoogleMap map;

        private final LatLng userLocation;
        private final float currentDiagonalDistance;
        private final float currentZoom;

        private MapZoomChecker(GoogleMap map, Collection<Marker> markers) {
            this.map = map;

            markerPositions = new ArrayList<>(markers.size());
            for (Marker marker : markers) {
                markerPositions.add(marker.getPosition());
            }

            userLocation = map.getCameraPosition().target;
            currentZoom = map.getCameraPosition().zoom;
            currentDiagonalDistance = LocationUtils.distanceInMeters(
                    map.getProjection().getVisibleRegion().farLeft,
                    map.getProjection().getVisibleRegion().nearRight);
        }

        @Override
        protected Float doInBackground(Void... params) {
            Collections.sort(markerPositions, new Comparator<LatLng>() {
                @Override
                public int compare(LatLng lhs, LatLng rhs) {
                    return Float.compare(
                            LocationUtils.distanceInMeters(lhs, userLocation),
                            LocationUtils.distanceInMeters(rhs, userLocation)
                    );
                }
            });

            LatLng marker = null;
            int numMinMarkers = Math.min(MIN_EVENTS_TO_SHOW, markerPositions.size());
            for (LatLng m : markerPositions) {
                numMinMarkers--;
                if (numMinMarkers <= 0) {
                    marker = m;
                    break;
                }
            }

            if (marker != null) {
                float minDiagonalDistance = DIAGONAL_DISTANCE_MULTIPLIER *
                        LocationUtils.distanceInMeters(marker, userLocation);
                if (currentDiagonalDistance < minDiagonalDistance) {
                    float zoomOutNeeded = (float) (Math.log(minDiagonalDistance / currentDiagonalDistance) / Math.log(2));
                    return Math.max(currentZoom - zoomOutNeeded, EventsMapsActivity.MIN_ZOOM_LEVEL);
                }
            }

            return -1f;
        }

        @Override
        protected void onPostExecute(Float zoom) {
            if (zoom > 0) {
                map.animateCamera(CameraUpdateFactory.zoomTo(zoom));
            }
        }
    }
}
