package com.eventshigh.nearme.app.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * This class describes one Event. Event have few attributes like title, category, location etc.
 */
public class Event implements Parcelable {
    // EH_RECOMMENDED is same as 100 people going to event.
    private static final int EH_RECOMMENDATION_BOOST = 100;

    public final String id;
    public final City city;
    public final String title;
    public final EventCategory category;

    public final String description;
    public final String[] tags;

    @Nullable public final String imgUrl;
    @Nullable public final String sourceUrl;
    @Nullable public final String bookingUrl;

    public final int numPeopleInterested;
    public final boolean ehRecommended;
    public final float uberScore;

    public final long[] eventTimings;    // each start time is stored as milliseconds since epoch.

    @Nullable public final LatLng location;
    @Nullable public final String venue;
    @Nullable public final String locality;
    @Nullable public final String address;

    @Nullable public final String organizerName;
    @Nullable public final String organizerPhone;
    @Nullable public final String organizerWebsite;

    public Event(String id, City city, String title, EventCategory category,
                 String description, String[] tags,
                 @Nullable String imgUrl, @Nullable String sourceUrl, @Nullable String bookingUrl,
                 int numPeopleInterested, boolean ehRecommended, float uberScore,
                 long[] eventTimings,
                 @Nullable LatLng location, @Nullable String venue, @Nullable String locality,
                 @Nullable String address,
                 String organizerName, String organizerPhone, String organizerWebsite) {
        this.id = id;
        this.city = city;
        this.title = title;
        this.category = category;

        this.description = description;
        this.tags = tags;

        this.imgUrl = Utils.checkIfUnknown(imgUrl);
        this.sourceUrl = Utils.checkIfUnknown(sourceUrl);
        this.bookingUrl = Utils.checkIfUnknown(bookingUrl);

        this.numPeopleInterested = numPeopleInterested;
        this.ehRecommended = ehRecommended;
        this.uberScore = uberScore;

        this.eventTimings = eventTimings;

        this.location = location != null && city.cityBounds.contains(location) ? location : null;
        this.venue = Utils.checkIfUnknown(venue);
        this.locality = Utils.checkIfUnknown(locality);
        this.address = Utils.checkIfUnknown(address);

        this.organizerName = Utils.checkIfUnknown(organizerName);
        this.organizerPhone = Utils.checkIfUnknown(organizerPhone);
        this.organizerWebsite = Utils.checkIfUnknown(organizerWebsite);
    }

    public Uri getEventDetailsURI() {
        return EventsHighEndpoints.getEventDetailsURI(this);
    }

    public int getPopularityScore() {
        return ehRecommended ? Math.max(EH_RECOMMENDATION_BOOST, numPeopleInterested) : numPeopleInterested;
    }

    public String getFullAddress() {
        return  (venue == null ? "" : venue + " " ) + (address == null ? "" : address).trim();
    }

    public String getShortAddress() {
        String shortAddress = (venue == null ? "" : venue + " " ) + (locality == null ? "" : "(" + locality +")").trim();
        return shortAddress.isEmpty() ? Utils.capitalize(city.name()) : shortAddress;
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
        dest.writeStringArray(tags);

        dest.writeString(emptyIfNull(imgUrl));
        dest.writeString(emptyIfNull(sourceUrl));
        dest.writeString(emptyIfNull(bookingUrl));

        dest.writeInt(numPeopleInterested);
        dest.writeBooleanArray(new boolean[]{ehRecommended});
        dest.writeFloat(uberScore);

        dest.writeLongArray(eventTimings);

        dest.writeParcelable(location == null ? new LatLng(0, 0) : location, flags);
        dest.writeString(emptyIfNull(venue));
        dest.writeString(emptyIfNull(locality));
        dest.writeString(emptyIfNull(address));

        dest.writeString(emptyIfNull(organizerName));
        dest.writeString(emptyIfNull(organizerPhone));
        dest.writeString(emptyIfNull(organizerWebsite));
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

                            in.readString(),
                            in.readString(),
                            in.readString(),

                            in.readInt(),
                            in.createBooleanArray()[0],
                            in.readFloat(),

                            in.createLongArray(),

                            (LatLng) in.readParcelable(LatLng.class.getClassLoader()),
                            in.readString(),
                            in.readString(),
                            in.readString(),

                            in.readString(),
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
        String description = eventJson.optString("description", "")
                .replaceAll("Â", "")
                .replaceAll("\\s+\n", "\n\n");

        JSONObject mashup = eventJson.optJSONObject("mashup");
        String source_url = eventJson.optString("source_url");
        String booking_url = mashup == null ? null : mashup.optString("booking_url");
        String img_url = eventJson.optString("img_url");
        if ((source_url != null && source_url.toLowerCase().contains("eventviva")) ||
            (img_url != null && img_url.endsWith("missing.png"))) {
            img_url = null;
        }

        int num_people_interested = eventJson.optInt("num_people_interested", 0);
        boolean eh_recommends = eventJson.optBoolean("eh_editor");
        float uberScore = (float) eventJson.optDouble("uber_score", 1);

        double lat = 0;
        double lon = 0;
        if (mashup != null) {
            lat = mashup.optDouble("lat", 0);
            lon = mashup.optDouble("lon", 0);
        }

        JSONObject localityJson = eventJson.optJSONObject("locality_info");
        if (!city.cityBounds.contains(new LatLng(lat, lon)) && localityJson != null) {
            // Invalid latitude and longitude. Try locality_info.
            lat = localityJson.optDouble("lat", 0);
            lon = localityJson.optDouble("lon", 0);
        }

        String address = null;
        JSONObject venueJson = eventJson.optJSONObject("venue_info");
        if (venueJson != null) {
            address = venueJson.optString("address");
        }

        String venue = null;
        if (mashup != null) {
            venue = Utils.checkIfUnknown(mashup.optString("venue_name"));
        }

        String locality = null;
        if (localityJson != null) {
            locality = localityJson.optString("locality");
        }

        if (address != null && venue != null && address.startsWith(venue)) {
            address = address.substring(venue.length()).trim();
        }

        // Tags.
        EventCategory category = EventCategory.OTHER;
        JSONArray tagsJsonArr = eventJson.getJSONArray("tags");
        ArrayList<String> tagsList = new ArrayList<>(tagsJsonArr.length());
        for (int j = 0; j < tagsJsonArr.length(); j++) {
            Object currentTag = tagsJsonArr.get(j);
            String tag = currentTag instanceof JSONObject ?
                    tagsJsonArr.getJSONObject(j).getString("tag") : String.valueOf(currentTag);
            tagsList.add(Utils.capitalize(tag));

            if (category == EventCategory.OTHER) {
                EventCategory tagCategory = EventCategory.parseCategory(tag);
                if (tagCategory != null) {
                    category = tagCategory;
                }
            }
        }

        // Event timings.
        List<Long> eventTimings = new ArrayList<>();
        Date todayMidnight = DateTimeUtils.toMidnight(Calendar.getInstance(), city.timeZone).getTime();
        Date eventTiming = DateTimeUtils.mergeDateTime(eventJson.optString("date"),
                eventJson.optString("start_time"), city.timeZone);
        if (eventTiming != null && eventTiming.after(todayMidnight)) {
            eventTimings.add(eventTiming.getTime());
        }

        JSONArray upcoming_occurrences = eventJson.optJSONArray("upcoming_occurrences");
        if (upcoming_occurrences != null) {
            for (int i = 0; i < upcoming_occurrences.length(); i++) {
                eventTiming =  DateTimeUtils.mergeDateTime(
                        upcoming_occurrences.getJSONObject(i).optString("date"),
                        upcoming_occurrences.getJSONObject(i).optString("start_time"), city.timeZone);
                if (eventTiming != null && eventTiming.after(todayMidnight) &&
                        !eventTimings.contains(eventTiming.getTime())) {
                    eventTimings.add(eventTiming.getTime());
                }
            }
        }

        if (eventTimings.size() > 2) {
            Collections.sort(eventTimings.subList(1, eventTimings.size()));
        }

        long[] eventTimingsArr = new long[eventTimings.size()];
        int i = 0;
        for (Long eventTime : eventTimings) {
            eventTimingsArr[i] = eventTime;
            i++;
        }

        // Organizer Info.
        String organizerName = mashup == null ? null : mashup.optString("organizer_name");
        String organizerPhone = mashup == null ? null : mashup.optString("organizer_phone");
        String organizerWebsite = mashup == null ? null : mashup.optString("organizer_website");

        return new Event(id,
                city,
                title,
                category,

                description,
                tagsList.toArray(new String[tagsList.size()]),

                img_url,
                source_url,
                booking_url,

                num_people_interested,
                eh_recommends,
                uberScore,

                eventTimingsArr,

                new LatLng(lat, lon),
                venue,
                locality,
                address,

                organizerName,
                organizerPhone,
                organizerWebsite
        );
    }

    public static List<Event> fromJSON(City city, JSONArray jsonArray, boolean includeWithoutLocation) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                Event event = fromJSON(city, jsonArray.getJSONObject(i));
                if (includeWithoutLocation || event.location != null) {
                    events.add(event);
                }
            } catch (JSONException | ParseException e) {
                Log.w(Event.class.getSimpleName(), "malformed JSON", e);
            }
        }
        return events;
    }

    public static EventsCollection parseUpcomingEvents(EventsContext eventsContext,
            EventsMarkerManager eventsMarkerManager, JSONObject eventsJSON, boolean includeWithoutLocation)
            throws JSONException {
        JSONArray upcomingEvents = eventsJSON.getJSONArray("upcoming_events");
        eventsMarkerManager.waitForLoading();
        EventsCollection.Builder builder =  new EventsCollection.Builder(eventsContext.city,
                eventsMarkerManager, false);
        List<Event> events = fromJSON(eventsContext.city, upcomingEvents, includeWithoutLocation);
        if (eventsContext.location != null) {
            Collections.sort(events, new EventComparator(eventsContext.location, eventsMarkerManager));
        }
        return builder.addAllEvent(events).build();
    }

    private static String emptyIfNull(@Nullable String string) {
        return (string == null ? "" : string);
    }
}
