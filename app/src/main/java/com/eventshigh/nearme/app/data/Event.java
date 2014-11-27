package com.eventshigh.nearme.app.data;

import android.net.Uri;
import android.util.Log;

import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class describes one Event. Event have few attributes like title, category, location etc.
 */
public class Event {
    // EH_RECOMMENDED is same as 100 people going to event.
    private static final int EH_RECOMMENDATION_BOOST = 100;
    private static final String EVENTS_HIGH_DETAIL_URI =
            "http://www.eventshigh.com/detail/CITY/ID?src=ehm";

    public final String id;
    public final String title;
    public final EventCategory category;
    public final String img_url;

    public final LatLng location;
    public final String locality;

    public final Date startTime;
    public final Date endTime;

    public final int numPeopleInterested;
    public final boolean ehRecommended;


    public Event(String id, String title, EventCategory category, String img_url,
                 int numPeopleInterested, boolean ehRecommended,
                 Date startTime, Date endTime,
                 LatLng location, String locality) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.img_url = img_url;

        this.numPeopleInterested = numPeopleInterested;
        this.ehRecommended = ehRecommended;

        this.location = location;
        this.locality = locality;

        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Uri getEventDetailsURI(City city) {
        return Uri.parse(EVENTS_HIGH_DETAIL_URI
                .replace("CITY", Utils.capitalize(city.toString()))
                .replace("ID", id));
    }

    public static List<Event> fromJSON(String jsonStr) throws JSONException, ParseException {
        List<Event> events = new ArrayList<Event>();

        JSONObject eventsJSON = new JSONObject(jsonStr);
        JSONArray upcomingEvents = eventsJSON.getJSONArray("upcoming_events");


        for (int i = 0; i < upcomingEvents.length(); i++) {
            try {
                JSONObject eventJson = upcomingEvents.getJSONObject(i);
                String id = eventJson.getString("id");
                String title = eventJson.getString("title");
                String img_url = checkIfUnknown(eventJson.getString("img_url"));

                int num_people_interested = eventJson.getInt("num_people_interested");
                boolean eh_recommends = eventJson.has("eh_recommends") &&
                        eventJson.getBoolean("eh_recommends");

                String date = eventJson.getString("date");
                String start_time = eventJson.getString("start_time");
                String end_time = eventJson.getString("end_time");

                JSONObject venue = eventJson.getJSONObject("venue_info");
                double lat = venue.getDouble("lat");
                double lon = venue.getDouble("lon");
                String locality = checkIfUnknown(venue.getString("name"));
                if (locality == null) {
                    locality = checkIfUnknown(eventJson.getString("locality"));
                }

                EventCategory category = EventCategory.OTHER;
                JSONArray tags = eventJson.getJSONArray("tags");
                for (int j = 0; category == EventCategory.OTHER && j < tags.length(); j++) {
                    try {
                        category = EventCategory.valueOf(
                                tags.getJSONObject(j).getString("tag").toUpperCase().replaceAll(" ", "_"));
                    } catch (IllegalArgumentException e) {
                        // Ignore. Unsupported category.
                    }
                }

                if (Math.abs(lat) < 1 || Math.abs(lon) < 1) {
                    // Invalid latitude and longitude.
                    // Ignore the entry.
                    continue;
                }

                Event event = new Event(id,
                        title,
                        category,
                        img_url,

                        num_people_interested,
                        eh_recommends,

                        Utils.mergeDateTime(date, start_time),
                        Utils.mergeDateTime(date, end_time),

                        new LatLng(lat, lon),
                        locality
                );
                events.add(event);
            } catch (JSONException ex) {
                // Malformed JSON, ignore.
                Log.w(Event.class.getSimpleName(), "malformed JSON", ex);
            }
        }

        return events;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object another) {
        return another instanceof Event &&
                id.equals(((Event) another).id);
    }

    public int getPopularityScore() {
        return ehRecommended ? Math.max(EH_RECOMMENDATION_BOOST, numPeopleInterested) : numPeopleInterested;
    }

    private static String checkIfUnknown(String string) {
        return (string == null ||
                string.isEmpty() ||
                string.equalsIgnoreCase("null") ||
                string.equalsIgnoreCase("unknown")
                ? null
                : string);
    }
}
