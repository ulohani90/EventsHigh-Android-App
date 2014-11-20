package com.eventshigh.nearme.app.data;

import com.eventshigh.nearme.app.Utils;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Event {
    // 20% boost for EH recommeded events.
    private static final double EH_RECOMMENDATION_BOOST = 1.2;

    public static enum Category {
        MUSIC,
        TECH,
        OTHER
    }

    public final String title;
    public final Category category;
    public final LatLng location;
    public final Date startTime;
    public final Date endTime;
    public final int numPeopleInterested;
    public final Double popularityScore;
    public final boolean ehRecommended;
    public final String locality;


    public Event(String title, Category category, LatLng location, Date startTime, Date endTime,
                 int numPeopleInterested, Double popularityScore, boolean ehRecommended, String locality) {
        this.title = title;
        this.category = category;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numPeopleInterested = numPeopleInterested;
        this.popularityScore = popularityScore;
        this.ehRecommended = ehRecommended;
        this.locality = locality;
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
            String locality = upcomingEvents.getJSONObject(i).getString("locality");

            // TODO: See if we can use tags and types from event JSON.
            // TODO: See if we can use any of following items.
            /**
             String description = upcomingEvents.getJSONObject(i).getString("description");
             String img_url = upcomingEvents.getJSONObject(i).getString("img_url");
             String source_url = upcomingEvents.getJSONObject(i).getString("source_url");
             String venue = upcomingEvents.getJSONObject(i).getString("venue");
             String category = upcomingEvents.getJSONObject(i).getString("category");
             **/


            Event event = new Event(title,
                    Category.OTHER,
                    new LatLng(lat, lon),
                    Utils.mergeDateTime(date, start_time),
                    Utils.mergeDateTime(date, end_time),
                    num_people_interested,
                    popularity_score / maxScore * (eh_recommends ? EH_RECOMMENDATION_BOOST : 1),
                    eh_recommends,
                    locality);
            events.add(event);
        }

        return events;
    }
}
