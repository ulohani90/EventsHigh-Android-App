package com.eventshigh.nearme.app.data;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.Utils;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Event {
    // 20% boost for EH recommeded events.
    private static final double EH_RECOMMENDATION_BOOST = 1.2;

    public static enum Category {
        ADVENTURE,
        ART,
        DANCE,
        FASHION,
        FOOD,
        FOR_COUPLES,
        FOR_FAMILY,
        FOR_KIDS,
        FOR_WOMEN,
        HEALTH,
        LITERATURE,
        MEETUPS,
        MUSIC,
        NIGHTLIFE,
        PHOTOGRAPHY,
        PLAYS,
        SPIRITUAL,
        SPORTS,
        TECH,
        WORKSHOPS,
        OTHER;

        public static Category fromString(String categoryString) {
            Category category = ALL_CATEGORIES.get(categoryString.toLowerCase());
            if (category == null) {
                return Category.OTHER;
            }

            return  category;
        }

        public int getIconStringId() {
            switch (this) {
                case ADVENTURE:
                    return R.string.fa_road;
                case ART:
                    return R.string.fa_photo;
                case DANCE:
                    return R.string.fa_paw;
                case FASHION:
                    return R.string.fa_eye;
                case FOOD:
                    return R.string.fa_cutlery;
                case FOR_COUPLES:
                    return R.string.fa_glass;
                case FOR_FAMILY:
                    return R.string.fa_home;
                case FOR_KIDS:
                    return R.string.fa_child;
                case FOR_WOMEN:
                    return R.string.fa_female;
                case HEALTH:
                    return R.string.fa_plus_square;
                case LITERATURE:
                    return R.string.fa_book;
                case MEETUPS:
                    return R.string.fa_users;
                case MUSIC:
                    return R.string.fa_music;
                case NIGHTLIFE:
                    return R.string.fa_moon_o;
                case PHOTOGRAPHY:
                    return R.string.fa_camera;
                case PLAYS:
                    return R.string.fa_paw;
                case SPIRITUAL:
                    return R.string.fa_empire;
                case SPORTS:
                    return R.string.fa_soccer_ball_o;
                case TECH:
                    return R.string.fa_linux;
                case WORKSHOPS:
                    return R.string.fa_institution;
            }

            return R.string.fa_calendar;
        }
    }

    private static Map<String, Category> ALL_CATEGORIES = new HashMap<String, Category>();
    static {
        for (Category category : Category.values()) {
            ALL_CATEGORIES.put(category.toString().replace('_', ' ').toLowerCase(), category);
        }
    }

    public final String title;
    public final Category category;
    public final LatLng location;
    public final Date startTime;
    public final Date endTime;
    public final int numPeopleInterested;
    public final Double popularityScore;
    public final boolean ehRecommended;


    public Event(String title, Category category, LatLng location, Date startTime, Date endTime,
                 int numPeopleInterested, Double popularityScore, boolean ehRecommended) {
        this.title = title;
        this.category = category;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numPeopleInterested = numPeopleInterested;
        this.popularityScore = popularityScore;
        this.ehRecommended = ehRecommended;
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

            Category category = Category.OTHER;
            JSONArray tags = upcomingEvents.getJSONObject(i).getJSONArray("tags");
            for (int j = 0; category == Category.OTHER && j < tags.length(); j++) {
                category = Category.fromString(tags.getJSONObject(j).getString("tag"));
            }

            /**
             String description = upcomingEvents.getJSONObject(i).getString("description");
             String locality = upcomingEvents.getJSONObject(i).getString("locality");
             String img_url = upcomingEvents.getJSONObject(i).getString("img_url");
             String source_url = upcomingEvents.getJSONObject(i).getString("source_url");
             **/

            Event event = new Event(title,
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
