package com.eventshigh.nearme.app.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

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
    public final City city;
    public final String title;
    public final EventCategory category;

    public final String description;
    public final String[] tagsWhiteList;
    public final String[] tagsOther;

    @Nullable public final String img_url;
    @Nullable public final String source_url;
    @Nullable public final String booking_url;

    public final int numPeopleInterested;
    public final boolean ehRecommended;

    @Nullable public final Date startTime;
    @Nullable public final Date endTime;

    public final LatLng location;
    @Nullable public final String venue;
    @Nullable public final String address;

    public Event(String id, City city, String title, EventCategory category,
                 String description, String[] tagsWhiteList, String[] tagsOther,
                 @Nullable String img_url, @Nullable String source_url, @Nullable String booking_url,
                 int numPeopleInterested, boolean ehRecommended,
                 @Nullable Date startTime, @Nullable Date endTime,
                 LatLng location, @Nullable String venue, @Nullable String address) {
        this.id = id;
        this.city = city;
        this.title = title;
        this.category = category;

        this.description = description;
        this.tagsWhiteList = tagsWhiteList;
        this.tagsOther = tagsOther;

        this.img_url = checkIfUnknown(img_url);
        this.source_url = checkIfUnknown(source_url);
        this.booking_url = checkIfUnknown(booking_url);

        this.numPeopleInterested = numPeopleInterested;
        this.ehRecommended = ehRecommended;

        this.startTime = startTime != null && startTime.getTime() > 0 ? startTime : null;
        this.endTime = endTime != null && endTime.getTime() > 0 ? endTime : null;

        this.location = location;
        this.venue = checkIfUnknown(venue);
        this.address = checkIfUnknown(address);
    }

    public Uri getEventDetailsURI() {
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
        dest.writeString(id);
        dest.writeString(city.toString());
        dest.writeString(title);
        dest.writeString(category.toString());

        dest.writeString(description);
        dest.writeStringArray(tagsWhiteList);
        dest.writeStringArray(tagsOther);

        dest.writeString(emptyIfNull(img_url));
        dest.writeString(emptyIfNull(source_url));
        dest.writeString(emptyIfNull(booking_url));

        dest.writeInt(numPeopleInterested);
        dest.writeBooleanArray(new boolean[]{ehRecommended});

        dest.writeLong(startTime == null ? 0 : startTime.getTime());
        dest.writeLong(endTime == null ? 0 : endTime.getTime());

        dest.writeParcelable(location, flags);
        dest.writeString(emptyIfNull(venue));
        dest.writeString(emptyIfNull(address));
    }

    // This is used to regenerate your object. All Parcelables must have
    // a CREATOR that implements these two methods
    public static final Parcelable.Creator<Event> CREATOR =
            new Parcelable.Creator<Event>() {
                public Event createFromParcel(Parcel in) {
                    return new Event(in.readString(),
                            City.valueOf(in.readString()),
                            in.readString(),
                            EventCategory.valueOf(in.readString()),

                            in.readString(),
                            in.createStringArray(),
                            in.createStringArray(),

                            in.readString(),
                            in.readString(),
                            in.readString(),

                            in.readInt(),
                            in.createBooleanArray()[0],

                            new Date(in.readLong()),
                            new Date(in.readLong()),

                            (LatLng) in.readParcelable(LatLng.class.getClassLoader()),
                            in.readString(),
                            in.readString()
                    );
                }

                public Event[] newArray(int size) {
                    return new Event[size];
                }
            };


    /**********************************
     Helper static methods, used for JSON parsing
     *********************************/
    public static Event fromJSON(City city, JSONObject eventJson) throws JSONException, ParseException {
        String id = eventJson.getString("id");
        String title = eventJson.getString("title");
        String description = eventJson.getString("description");

        String img_url = eventJson.getString("img_url");
        if (img_url.contains("eventviva") || img_url.endsWith("missing.png")) {
            img_url = null;
        }
        String source_url = eventJson.getString("source_url");
        String booking_url = eventJson.getString("booking_url");

        int num_people_interested = eventJson.getInt("num_people_interested");
        boolean eh_recommends = eventJson.has("eh_editor") && eventJson.getBoolean("eh_editor");

        String date = eventJson.getString("date");
        String start_time = eventJson.getString("start_time");
        String end_time = null; // eventJson.getString("end_time");

        double lat = 0;
        double lon = 0;
        JSONObject venueJson = eventJson.optJSONObject("venue_info");
        JSONObject localityJson = eventJson.optJSONObject("locality_info");
        if (venueJson != null) {
            lat = venueJson.getDouble("lat");
            lon = venueJson.getDouble("lon");
        }

        if (Math.abs(lat) < 1 && Math.abs(lon) < 1 && localityJson != null) {
            // Invalid latitude and longitude. Try locality_info.
            lat = localityJson.getDouble("lat");
            lon = localityJson.getDouble("lon");
        }

        if (Math.abs(lat) < 1 && Math.abs(lon) < 1) {
            // Invalid latitude and longitude.
            // Ignore the entry.
            throw new ParseException("invalid latitude and longitude for " + id, 0);
        }

        String venue = null;
        String address = null;
        if (venueJson != null) {
            venue =  checkIfUnknown(venueJson.optString("name"));
            address = venueJson.optString("address");
        }

        if (venue == null && localityJson != null) {
            venue = localityJson.optString("locality");
        }

        EventCategory category = EventCategory.OTHER;
        JSONArray tagsJsonArr = eventJson.getJSONArray("tags");
        ArrayList<String> tagsWhiteList = new ArrayList<>(tagsJsonArr.length());
        ArrayList<String> otherTags = new ArrayList<>(tagsJsonArr.length());
        for (int j = 0; j < tagsJsonArr.length(); j++) {
            String tag = tagsJsonArr.getJSONObject(j).getString("tag");
            String tagU = tag.toUpperCase().replaceAll(" ", "_").replaceAll("&_", "");
            String tagToShow = Utils.capitalize(tag);

            try {
                EventCategory tagCategory = EventCategory.valueOf(tagU);
                if (category == EventCategory.OTHER) {
                    category = tagCategory;
                    tagsWhiteList.add(0, tagToShow);
                } else {
                    tagsWhiteList.add(tagToShow);
                }
                continue;
            } catch (IllegalArgumentException e) {
                // Ignore. Unsupported category.
            }

            try {
                TagsWhiteList.valueOf(tagU);
                tagsWhiteList.add(tagToShow);
                continue;
            } catch (IllegalArgumentException e) {
                // Ignore. Not a whitelisted category.
            }

            otherTags.add(tagToShow);
        }

        return new Event(id,
                city,
                title,
                category,

                description,
                tagsWhiteList.toArray(new String[tagsWhiteList.size()]),
                otherTags.toArray(new String[otherTags.size()]),

                img_url,
                source_url,
                booking_url,

                num_people_interested,
                eh_recommends,

                Utils.mergeDateTime(date, start_time, city.timeZone),
                Utils.mergeDateTime(date, end_time, city.timeZone),

                new LatLng(lat, lon),
                venue,
                address);
    }

    public static List<Event> fromJSON(City city, JSONArray jsonArray) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                events.add(fromJSON(city, jsonArray.getJSONObject(i)));
            } catch (JSONException | ParseException e) {
                // Log.w(Event.class.getSimpleName(), "malformed JSON", e);
            }
        }
        return events;
    }

    public static List<Event> parseUpcomingEvents(City city, String jsonStr) throws JSONException {
        JSONObject eventsJSON = new JSONObject(jsonStr);
        JSONArray upcomingEvents = eventsJSON.getJSONArray("upcoming_events");
        return fromJSON(city, upcomingEvents);
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
