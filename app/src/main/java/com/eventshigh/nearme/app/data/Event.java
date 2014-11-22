package com.eventshigh.nearme.app.data;

import android.net.Uri;

import com.eventshigh.nearme.app.Utils;
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
    // 20% boost for EH recommeded events.
    private static final double EH_RECOMMENDATION_BOOST = 1.2;
    private static final String EVENTS_HIGH_DETAIL_URI = "http://www.eventshigh.com/detail/CITY/ID";

    public final String id;
    public final String title;
    public final EventCategory category;
    public final LatLng location;
    public final Date startTime;
    public final Date endTime;
    public final int numPeopleInterested;
    public final Double popularityScore;
    public final boolean ehRecommended;


    public Event(String id, String title, EventCategory category, LatLng location, Date startTime, Date endTime,
                 int numPeopleInterested, Double popularityScore, boolean ehRecommended) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numPeopleInterested = numPeopleInterested;
        this.popularityScore = popularityScore;
        this.ehRecommended = ehRecommended;
    }

    public Uri getEventDetailsURI(City city) {
        return Uri.parse(EVENTS_HIGH_DETAIL_URI
                .replace("CITY", city.toString())
                .replace("ID", id));
    }

    public static List<Event> fromJSON(String jsonStr) throws JSONException, ParseException {
        List<Event> events = new ArrayList<Event>();

        JSONObject eventsJSON = new JSONObject(jsonStr);
        JSONArray upcomingEvents = eventsJSON.getJSONArray("upcoming_events");

        // Get Max popularity score so that we can normalize everything else.
        // Give 20% boost to recommended events.
        double maxScore = 0;
        for (int i = 0; i < upcomingEvents.length(); i++) {
            double popularity_score = upcomingEvents.getJSONObject(i).getDouble("popularity_score");
            if (maxScore < popularity_score) {
                maxScore = popularity_score;
            }
        }
        maxScore *= EH_RECOMMENDATION_BOOST;


        for (int i = 0; i < upcomingEvents.length(); i++) {
            String id = upcomingEvents.getJSONObject(i).getString("id");
            String title = upcomingEvents.getJSONObject(i).getString("title");
            int num_people_interested = upcomingEvents.getJSONObject(i).getInt("num_people_interested");
            double popularity_score = upcomingEvents.getJSONObject(i).getDouble("popularity_score");
            double lat = upcomingEvents.getJSONObject(i).getJSONObject("venue_info").getDouble("lat");
            double lon = upcomingEvents.getJSONObject(i).getJSONObject("venue_info").getDouble("lon");
            String date = upcomingEvents.getJSONObject(i).getString("date");
            String start_time = upcomingEvents.getJSONObject(i).getString("start_time");
            String end_time = upcomingEvents.getJSONObject(i).getString("end_time");
            boolean eh_recommends = upcomingEvents.getJSONObject(i).has("eh_recommends") &&
                    upcomingEvents.getJSONObject(i).getBoolean("eh_recommends");

            EventCategory category = EventCategory.OTHER;
            JSONArray tags = upcomingEvents.getJSONObject(i).getJSONArray("tags");
            for (int j = 0; category == EventCategory.OTHER && j < tags.length(); j++) {
                category = EventCategory.fromString(tags.getJSONObject(j).getString("tag"));
            }

            /**
             String description = upcomingEvents.getJSONObject(i).getString("description");
             String locality = upcomingEvents.getJSONObject(i).getString("locality");
             String img_url = upcomingEvents.getJSONObject(i).getString("img_url");
             String source_url = upcomingEvents.getJSONObject(i).getString("source_url");
             **/

            Event event = new Event(id,
                    title,
                    category,
                    new LatLng(lat, lon),
                    Utils.mergeDateTime(date, start_time),
                    Utils.mergeDateTime(date, end_time),
                    num_people_interested,
                    popularity_score / maxScore * (eh_recommends ? EH_RECOMMENDATION_BOOST : 1),
                    eh_recommends);
            events.add(event);
        }

        return events;
    }
}
