package com.eventshigh.nearme.app.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
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
public class Event implements Parcelable {
    // EH_RECOMMENDED is same as 100 people going to event.
    private static final int EH_RECOMMENDATION_BOOST = 100;
    private static final String EVENTS_HIGH_DETAIL_URI =
            "http://www.eventshigh.com/detail/CITY/ID?src=ehm";

    public final String id;
    public final String title;
    public final EventCategory category;
    @Nullable public final String img_url;

    public final int numPeopleInterested;
    public final boolean ehRecommended;

    @Nullable public final Date startTime;
    @Nullable public final Date endTime;

    public final LatLng location;
    @Nullable public final String venue;

    public Event(String id, String title, EventCategory category, @Nullable String img_url,
                 int numPeopleInterested, boolean ehRecommended,
                 @Nullable Date startTime, @Nullable Date endTime,
                 LatLng location, @Nullable String venue) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.img_url = img_url;

        this.numPeopleInterested = numPeopleInterested;
        this.ehRecommended = ehRecommended;

        this.startTime = startTime;
        this.endTime = endTime;

        this.location = location;
        this.venue = venue;
    }

    public Uri getEventDetailsURI(City city) {
        return Uri.parse(EVENTS_HIGH_DETAIL_URI
                .replace("CITY", Utils.capitalize(city.toString()))
                .replace("ID", id));
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


    /**********************************
     Parcel management methods.
     *********************************/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBooleanArray(new boolean[] { ehRecommended});

        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(category.toString());
        dest.writeString(emptyIfNull(img_url));

        dest.writeInt(numPeopleInterested);

        dest.writeLong(startTime == null ? 0 : startTime.getTime());
        dest.writeLong(endTime == null ? 0 : endTime.getTime());

        dest.writeParcelable(location, flags);
        dest.writeString(emptyIfNull(venue));
    }

    // This is used to regenerate your object. All Parcelables must have
    // a CREATOR that implements these two methods
    public static final Parcelable.Creator<Event> CREATOR =
            new Parcelable.Creator<Event>() {
                public Event createFromParcel(Parcel in) {
                    boolean[] boolArray = new boolean[1];
                    in.readBooleanArray(boolArray);

                    return new Event(in.readString(),
                            in.readString(),
                            EventCategory.valueOf(in.readString()),
                            checkIfUnknown(in.readString()),

                            in.readInt(),
                            boolArray[0],

                            new Date(in.readLong()),
                            new Date(in.readLong()),

                            (LatLng) in.readParcelable(LatLng.class.getClassLoader()),
                            checkIfUnknown(in.readString())
                    );
                }

                public Event[] newArray(int size) {
                    return new Event[size];
                }
            };


    /**********************************
     Helper static methods, used for JSON parsing
     *********************************/

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

                double lat = 0;
                double lon = 0;
                JSONObject venueJson = null;
                if (eventJson.has("venue_info")) {
                    venueJson = eventJson.getJSONObject("venue_info");
                    lat = venueJson.getDouble("lat");
                    lon = venueJson.getDouble("lon");
                }

                if (Math.abs(lat) < 1 || Math.abs(lon) < 1) {
                    // Invalid latitude and longitude. Try locality_info.
                    if (eventJson.has("locality_info")) {
                        JSONObject locality = eventJson.getJSONObject("locality_info");
                        lat = locality.getDouble("lat");
                        lon = locality.getDouble("lon");
                    }
                }

                if (Math.abs(lat) < 1 || Math.abs(lon) < 1) {
                    // Invalid latitude and longitude.
                    // Ignore the entry.
                    continue;
                }

                String venue = venueJson == null ? null : checkIfUnknown(venueJson.getString("name"));
                if (venue == null) {
                    venue = checkIfUnknown(eventJson.getString("locality"));
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

                Event event = new Event(id,
                        title,
                        category,
                        img_url,

                        num_people_interested,
                        eh_recommends,

                        Utils.mergeDateTime(date, start_time),
                        Utils.mergeDateTime(date, end_time),

                        new LatLng(lat, lon),
                        venue
                );
                events.add(event);
            } catch (JSONException ex) {
                // Malformed JSON, ignore.
                Log.w(Event.class.getSimpleName(), "malformed JSON", ex);
            }
        }

        return events;
    }

    private static String checkIfUnknown(String string) {
        return (string == null ||
                string.isEmpty() ||
                string.equalsIgnoreCase("null") ||
                string.equalsIgnoreCase("unknown")
                ? null
                : string);
    }

    private static String emptyIfNull(@Nullable String string) {
        return (string == null ? "" : string);
    }
}
