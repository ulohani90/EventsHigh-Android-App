package com.eventshigh.nearme.app.data;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class describes one Event. Event have few attributes like title, category, location etc.
 */
public class Event implements Parcelable {
    private static final Pattern YOUTUBE_FINDER = Pattern.compile("www.youtube.com/embed/([^\"]*)\"");

    public final String id;
    public final City city;
    public final String title;
    public final EventCategory category;

    public final String description;
    public final ArrayList<String> tags;
    @Nullable
    public final String youtubeVideoId;

    @Nullable
    public final String imgUrl;
    @Nullable
    public final String sourceUrl;
    @Nullable
    public final String bookingUrl;
    @Nullable
    public final String bookingText;

    public final int numViews;
    public final int numSaves;
    public final boolean ehRecommended;
    public final float uberScore;

    public final long[] eventTimings;    // each start time is stored as milliseconds since epoch.

    @Nullable
    public final LatLng location;
    @Nullable
    public final String venue;
    @Nullable
    public final String locality;
    @Nullable
    public final String address;
    public final boolean isCleanVenue;

    public final String[] performers;

    @Nullable
    public final String organizerName;
    @Nullable
    public final String organizerPhone;
    @Nullable
    public final String organizerWebsite;
    @Nullable
    public final String organizerEmail;
    @Nullable
    public final String organizerLink;

    public final double minPrice;
    public final double maxPrice;
    @Nullable
    public final String currency;
    @Nullable
    public final String priceName;
    @Nullable
    public final String priceNote;

    public final ArrayList<EhPrices> ehPrices;

    public final EventDescriptionSection[] descriptionSections;

    public final ArrayList<MovieUserReviewObject> reviewObjects;

    public Event(String id, City city, String title, EventCategory category,
                 String description, ArrayList<String> tags, @Nullable String youtubeVideoId,
                 @Nullable String imgUrl, @Nullable String sourceUrl,
                 @Nullable String bookingUrl, @Nullable String bookingText,
                 int numViews, int numSaves, boolean ehRecommended,
                 float uberScore, long[] eventTimings,
                 @Nullable LatLng location, @Nullable String venue, @Nullable String locality,
                 @Nullable String address, boolean isCleanVenue,
                 String[] performers,
                 String organizerName, String organizerPhone, String organizerWebsite,
                 String organizerEmail, String organizerLink, ArrayList<EhPrices> ehPrices,
                 double minPrice, double maxPrice, @Nullable String currency, String priceName, String priceNote,
                 EventDescriptionSection[] descriptionSections, ArrayList<MovieUserReviewObject> reviewObjects) {
        this.id = id;
        this.city = city;
        this.title = title;
        this.category = category;

        this.description = description;
        this.tags = tags;
        this.youtubeVideoId = Utils.checkIfUnknown(youtubeVideoId);

        this.imgUrl = Utils.checkIfUnknown(imgUrl);
        this.sourceUrl = Utils.checkIfUnknown(sourceUrl);
        this.bookingUrl = Utils.checkIfUnknown(bookingUrl);
        this.bookingText = Utils.checkIfUnknown(bookingText);

        this.numViews = numViews;
        this.numSaves = numSaves;
        this.ehRecommended = ehRecommended;
        this.uberScore = uberScore;

        this.eventTimings = eventTimings;

        this.location = location != null && city.cityBounds.contains(location) ? location : null;
        this.venue = Utils.checkIfUnknown(venue);
        this.locality = Utils.checkIfUnknown(locality);
        this.address = Utils.checkIfUnknown(address);
        this.isCleanVenue = venue != null && isCleanVenue;

        this.performers = performers;

        this.organizerName = Utils.checkIfUnknown(organizerName);
        this.organizerPhone = Utils.checkIfUnknown(organizerPhone);
        this.organizerWebsite = Utils.checkIfUnknown(organizerWebsite);
        this.organizerEmail = Utils.checkIfUnknown(organizerEmail);
        this.organizerLink = Utils.checkIfUnknown(organizerLink);

        this.ehPrices = ehPrices;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.currency = Utils.checkIfUnknown(currency);
        this.priceName = priceName;
        this.priceNote = priceNote;

        this.descriptionSections = descriptionSections;
        this.reviewObjects = reviewObjects;
    }


    public Event(Parcel in) {
        this.id = in.readString();
        this.city = City.valueOf(in.readString());
        this.title = in.readString();
        this.category = EventCategory.valueOf(in.readString());

        this.description = in.readString();
        this.tags = new ArrayList<>();
        in.readStringList(tags);
        this.youtubeVideoId = Utils.checkIfUnknown(in.readString());

        this.imgUrl = Utils.checkIfUnknown(in.readString());
        this.sourceUrl = Utils.checkIfUnknown(in.readString());
        this.bookingUrl = Utils.checkIfUnknown(in.readString());
        this.bookingText = Utils.checkIfUnknown(in.readString());

        this.numViews = in.readInt();
        this.numSaves = in.readInt();
        this.ehRecommended = in.createBooleanArray()[0];
        this.uberScore = in.readFloat();

        this.eventTimings = in.createLongArray();
        LatLng sLocation = (LatLng) in.readParcelable(LatLng.class.getClassLoader());
        this.location = sLocation != null && city.cityBounds.contains(sLocation) ? sLocation : null;
        this.venue = Utils.checkIfUnknown(in.readString());
        this.locality = Utils.checkIfUnknown(in.readString());
        this.address = Utils.checkIfUnknown(in.readString());
        this.isCleanVenue = venue != null && in.createBooleanArray()[0];

        this.performers = in.createStringArray();

        this.organizerName = Utils.checkIfUnknown(in.readString());
        this.organizerPhone = Utils.checkIfUnknown(in.readString());
        this.organizerWebsite = Utils.checkIfUnknown(in.readString());
        this.organizerEmail = Utils.checkIfUnknown(in.readString());
        this.organizerLink = Utils.checkIfUnknown(in.readString());

        ehPrices = new ArrayList<>();
        in.readTypedList(ehPrices, EhPrices.CREATOR);
        this.minPrice = in.readDouble();
        this.maxPrice = in.readDouble();
        this.currency = Utils.checkIfUnknown(in.readString());
        this.priceName = in.readString();
        this.priceNote = in.readString();
        this.descriptionSections = in.createTypedArray(EventDescriptionSection.CREATOR);
        reviewObjects = new ArrayList<>();
        in.readTypedList(reviewObjects, MovieUserReviewObject.CREATOR);
    }

    public Uri getEventDetailsURI() {
        return EventsHighEndpoints.getEventDetailsURI(this);
    }

    public Uri getEventShareURI() {
        return getEventShareURI(null);
    }

    public Uri getEventShareURI(@Nullable String src) {
        return EventsHighEndpoints.getEventShareURI(this, src);
    }

    public String getFullAddress() {
        return (venue == null ? "" : venue + " ") + (address == null ? "" : address).trim();
    }

    public String getShortAddress() {
        String shortAddress = (venue == null ? "" : venue + " ") + (locality == null ? "" : "(" + locality + ")").trim();
        return shortAddress.isEmpty() ? Utils.capitalize(city.name()) : shortAddress;
    }

    public
    @Nullable
    String getPriceString() {
        if (minPrice < 0 || maxPrice < 0) {
            return null;
        }

        if (minPrice < 0.01 && maxPrice < 0.01) {
            return "Free";
        }

        if (maxPrice - minPrice < 0.01) {
            return currency + " " + Math.round(minPrice);
        }

        return currency + " " + Math.round(minPrice) + " - " + Math.round(maxPrice);
    }

    public
    @Nullable
    String getPriceString(double minPrice, double maxPrice, String currency) {
        if (minPrice < 0 || maxPrice < 0) {
            return null;
        }

        if (minPrice < 0.01 && maxPrice < 0.01) {
            return "Free";
        }

        if (maxPrice - minPrice < 0.01) {
            return currency + " " + Math.round(minPrice);
        }

        return currency + " " + Math.round(minPrice) + " - " + Math.round(maxPrice);
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
     * Parcel management methods.
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
        dest.writeStringList(tags);
        dest.writeString(emptyIfNull(youtubeVideoId));

        dest.writeString(emptyIfNull(imgUrl));
        dest.writeString(emptyIfNull(sourceUrl));
        dest.writeString(emptyIfNull(bookingUrl));
        dest.writeString(emptyIfNull(bookingText));

        dest.writeInt(numViews);
        dest.writeInt(numSaves);
        dest.writeBooleanArray(new boolean[]{ehRecommended});
        dest.writeFloat(uberScore);

        dest.writeLongArray(eventTimings);

        dest.writeParcelable(location == null ? new LatLng(0, 0) : location, flags);
        dest.writeString(emptyIfNull(venue));
        dest.writeString(emptyIfNull(locality));
        dest.writeString(emptyIfNull(address));
        dest.writeBooleanArray(new boolean[]{isCleanVenue});

        dest.writeStringArray(performers);

        dest.writeString(emptyIfNull(organizerName));
        dest.writeString(emptyIfNull(organizerPhone));
        dest.writeString(emptyIfNull(organizerWebsite));
        dest.writeString(emptyIfNull(organizerEmail));
        dest.writeString(emptyIfNull(organizerLink));

        dest.writeTypedList(ehPrices);
        dest.writeDouble(minPrice);
        dest.writeDouble(maxPrice);
        dest.writeString(emptyIfNull(currency));
        dest.writeString(priceName);
        dest.writeString(priceNote);
        dest.writeTypedArray(descriptionSections, flags);
        dest.writeTypedList(reviewObjects);
    }

    // This is used to regenerate your object. All Parcelables must have
    // a CREATOR that implements these two methods
    public static final Parcelable.Creator<Event> CREATOR =
            new Parcelable.Creator<Event>() {
                public Event createFromParcel(Parcel in) {
                    return new Event(in);

                }

                public Event[] newArray(int size) {
                    return new Event[size];
                }
            };


    /**********************************
     * Helper static methods, used for JSON parsing
     *********************************/
    public static Event fromJSON(JSONObject eventJson) throws JSONException, ParseException {
        if (eventJson.optBoolean("junk")) {
            // Junk event.
            throw new ParseException("junk event", 0);
        }

        String id = eventJson.getString("id");
        String title = eventJson.getString("title");
        String description = eventJson.optString("description")
                .replaceAll("Â", "")
                .replaceAll("\r\n", "<br/>")
                .replaceAll("\n\n", "<br/><br/>");
        City city = City.valueOf(eventJson.getString("city").toUpperCase());

        JSONObject mashup = eventJson.optJSONObject("mashup");
        String source_url = eventJson.optString("source_url");
        String booking_url = mashup == null ? null : mashup.optString("booking_url");
        String booking_text = eventJson.optString("booking_text");
        String img_url = eventJson.optString("img_url");
        if ((source_url != null && source_url.toLowerCase().contains("eventviva")) ||
                (img_url != null && img_url.endsWith("missing.png"))) {
            img_url = null;
        }

        JSONObject stats = eventJson.optJSONObject("stats");
        int num_views = stats == null ? 0 : stats.optInt("view_event");
        int num_saves = stats == null ? 0 : stats.optInt("add_favorite");
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

        String venue = null;
        String address = null;
        boolean isCleanVenue = false;
        JSONObject venueJson = eventJson.optJSONObject("venue_info");
        if (venueJson != null) {
            venue = Utils.capitalize(venueJson.optString("name"));
            address = venueJson.optString("address");
            isCleanVenue = venueJson.optBoolean("clean_venue", false);
        }

        String locality = null;
        if (localityJson != null) {
            locality = localityJson.optString("locality");
        }

        if (address != null && venue != null &&
                address.toLowerCase().startsWith(venue.toLowerCase())) {
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
            if (tag.equalsIgnoreCase("featured")) {
                continue;
            }
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
        Date eventTiming = DateTimeUtils.mergeDateTime(eventJson.optString("date"),
                eventJson.optString("start_time"), city.timeZone);
        if (eventTiming != null) {
            eventTimings.add(eventTiming.getTime());
        }

        JSONArray upcoming_occurrences = eventJson.optJSONArray("upcoming_occurrences");
        if (upcoming_occurrences != null) {
            for (int i = 0; i < upcoming_occurrences.length(); i++) {
                eventTiming = DateTimeUtils.mergeDateTime(
                        upcoming_occurrences.getJSONObject(i).optString("date"),
                        upcoming_occurrences.getJSONObject(i).optString("start_time"), city.timeZone);
                if (eventTiming != null && !eventTimings.contains(eventTiming.getTime())) {
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

        // Performers
        JSONArray participantsInfo = eventJson.optJSONArray("participants");
        List<String> performers = new ArrayList<>(participantsInfo == null ? 0 : participantsInfo.length());
        if (participantsInfo != null) {
            for (i = 0; i < participantsInfo.length(); i++) {
                String performer = participantsInfo.getJSONObject(i).optString("name");
                if (performer != null) {
                    performers.add(performer);
                }
            }
        }

        // Organizer Info.
        String organizerName = mashup == null ? null : mashup.optString("organizer_name");
        String organizerPhone = mashup == null ? null : mashup.optString("organizer_phone");
        String organizerWebsite = mashup == null ? null : mashup.optString("organizer_website");
        String organizerEmail = mashup == null ? null : mashup.optString("organizer_email");
        String organizerLink = mashup == null ? null : mashup.optString("organizer_link");

        // Price.
        double minPrice = -1, maxPrice = -1;
        String ehPriceName = "";
        String ehPriceNote = "";
        String currency = "\u20B9";
        JSONArray prices = eventJson.optJSONArray("eh_prices");
        ArrayList<EhPrices> ehPrices = new ArrayList<>();
        if (prices != null) {

            for (int j = 0; j < prices.length(); j++) {
                minPrice = -1;
                maxPrice = -1;
                JSONObject ehPrice = prices.getJSONObject(j);
                currency = ehPrice.optString("currency", "\u20B9");
                if (currency.equalsIgnoreCase("INR")) {
                    currency = "\u20B9";
                }
                ArrayList<Long> ehOccurences = new ArrayList<>();
                JSONArray occurrences = ehPrice.getJSONArray("occurrences");
                if (occurrences != null) {
                    for (int k = 0; k < occurrences.length(); k++) {
                        Date date = DateTimeUtils.mergeDateTime(occurrences.getJSONObject(k).optString("date"),
                                occurrences.getJSONObject(k).optString("time"), city.timeZone);
                        ehOccurences.add(date.getTime());
                    }
                }
                ehPriceName = ehPrice.optString("name", "");
                ehPriceNote = ehPrice.optString("note", "");
                double discountValue = ehPrice.optDouble("discount_value", -1);
                double value = ehPrice.optDouble("value", -1);
                /*if (value < 0.01) {
                    value = ehPrice.optDouble("value", -1);
                }*/
                if (discountValue > 0) {
                    minPrice = minPrice < 0 ? value : Math.min(minPrice, value);
                    maxPrice = maxPrice < 0 ? value : Math.max(maxPrice, value);
                } else if (value > 0) {
                    minPrice = minPrice < 0 ? value : Math.min(minPrice, value);
                    maxPrice = maxPrice < 0 ? value : Math.max(maxPrice, value);
                } else {
                    minPrice = 0;
                    maxPrice = 0;

                }
                ehPrices.add(EhPrices.createObject(minPrice, maxPrice, ehPriceName, ehPriceNote, currency, value, discountValue, ehOccurences,0));

            }
        }

        if (mashup != null && ehPrices.size() == 0) {
            JSONObject priceInfo = mashup.optJSONObject("price_info");
            if (priceInfo != null) {
                if (priceInfo.optString("type").equalsIgnoreCase("free")) {
                    minPrice = 0;
                    maxPrice = 0;
                } else {
                    currency = priceInfo.optString("currency", "\u20B9");
                    if (currency.equalsIgnoreCase("INR")) {
                        currency = "\u20B9";
                    }
                    minPrice = priceInfo.optDouble("min", -1);
                    maxPrice = priceInfo.optDouble("max", -1);
                    if (minPrice < 0 || maxPrice < 0) {
                        double value = priceInfo.optDouble("value", -1);
                        if (value > 0) {
                            minPrice = value;
                            maxPrice = value;
                        }
                    }
                }
                ehPriceName = priceInfo.optString("name", "");

            }
        }

        //User Reviews

        ArrayList<MovieUserReviewObject> reviews = new ArrayList<>();
        JSONArray reviewsArray = eventJson.getJSONArray("reviews");
        if (reviewsArray != null) {
            for (int l = 0; l < reviewsArray.length(); l++) {
                MovieUserReviewObject obj = new MovieUserReviewObject(reviewsArray.getJSONObject(l));
                if (obj.getReviewState() == null || obj.getReviewState().equalsIgnoreCase("published")) {
                    reviews.add(obj);
                }
            }
        }

        // Event Description Sections.
        List<EventDescriptionSection> descriptionSections = new ArrayList<>();
        JSONArray descriptionSectionsJson = eventJson.optJSONArray("description_sections");
        if (descriptionSectionsJson != null) {
            descriptionSections = EventDescriptionSection.fromJSON(descriptionSectionsJson);
        }

        // Find youtube id if any.
        String youtubeId = null;
        Matcher youtubeMatcher = YOUTUBE_FINDER.matcher(description);
        if (youtubeMatcher.find()) {
            youtubeId = youtubeMatcher.group(1);
        }
        if (youtubeId == null) {
            for (EventDescriptionSection descriptionSection : descriptionSections) {
                youtubeMatcher = YOUTUBE_FINDER.matcher(descriptionSection.description);
                if (youtubeMatcher.find()) {
                    youtubeId = youtubeMatcher.group(1);
                    break;
                }
            }
        }

        return new Event(id,
                city,
                title,
                category,

                description,
                tagsList,
                youtubeId,

                img_url,
                source_url,
                booking_url,
                booking_text,

                num_views,
                num_saves,
                eh_recommends,
                uberScore,

                eventTimingsArr,

                new LatLng(lat, lon),
                venue,
                locality,
                address,
                isCleanVenue,

                performers.toArray(new String[performers.size()]),

                organizerName,
                organizerPhone,
                organizerWebsite,
                organizerEmail,
                organizerLink,
                ehPrices,
                minPrice,
                maxPrice,
                currency,
                ehPriceName,
                ehPriceNote,
                descriptionSections.toArray(new EventDescriptionSection[descriptionSections.size()]),
                reviews
        );
    }

    public static List<Event> fromJSON(JSONArray jsonArray, boolean includeWithoutLocation) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                Event event = fromJSON(jsonArray.getJSONObject(i));
                if (includeWithoutLocation || event.location != null) {
                    events.add(event);
                }
            } catch (JSONException | ParseException e) {
                Crashlytics.getInstance().core.logException(e);
            }
        }
        return events;
    }

    public static List<Event> parseUpcomingEvents(JSONObject eventsJSON,
                                                  boolean includeWithoutLocation) throws JSONException {
        JSONArray upcomingEvents = eventsJSON.getJSONArray("upcoming_events");
        return fromJSON(upcomingEvents, includeWithoutLocation);
    }


    private static String emptyIfNull(@Nullable String string) {
        return (string == null ? "" : string);
    }

    public
    @Nullable
    String getMapQuery() {
        String query = isCleanVenue && venue != null ?
                (venue.toLowerCase().contains(city.toString().toLowerCase()) ?
                        venue :
                        venue + " " + city.toString().toLowerCase())
                : getFullAddress();
        if (query == null || query.isEmpty()) {
            if (location == null) {
                return null;
            }
            query = location.latitude + "," + location.longitude + " (" + title + ")";
        }

        return query;
    }

    public Intent getShowDirectionsOnMapIntent() {
        String query = getMapQuery();
        if (query == null) {
            return null;
        }

        // From https://developers.google.com/maps/documentation/android/intents
        Uri locationUri = Uri.parse("google.navigation:q=" + query);
        Intent mapIntent = new Intent(android.content.Intent.ACTION_VIEW, locationUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        return mapIntent;
    }

}
