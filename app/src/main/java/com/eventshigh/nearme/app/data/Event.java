package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.stream.AdditionalTicketField;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class describes one Event. Event have few attributes like title, category, location etc.
 */
public class Event implements Parcelable {
    private static final Pattern YOUTUBE_FINDER = Pattern.compile("www.youtube.com/embed/([^\"]*)\"");

    private static final String CONSTANT_DISCOUNT_PERCENTAGE = "Discount Percentage";
    private static final String CONSTANT_DISCOUNT_PERCENTAGE_TEXT = "offer message";

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

    public final ArrayList<String> allImages;
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

    public final List<Long> eventTimings;    // each start time is stored as milliseconds since epoch.

    @Nullable
    public final LatLng location;
    @Nullable
    public final String venue;
    @Nullable
    public final String locality;
    @Nullable
    public final String address;
    public final boolean isCleanVenue;


    public final List<String> performers;

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

    public final String organizerAccountName;

    public final double minPrice;
    public final double maxPrice;
    @Nullable
    public final String currency;
    @Nullable
    public final String priceName;
    @Nullable
    public final String priceNote;

    public final ArrayList<EhPrices> ehPrices;

    public final List<EventDescriptionSection> descriptionSections;

    public final ArrayList<MovieUserReviewObject> reviewObjects;

    @Nullable
    public final String requestPerAttendeeData;
    @Nullable
    public final List<AdditionalTicketField> additionalTicketFieldList;

    public final List<EventSession> sessions;

    public final String sessionTitlePhrase;

    public final boolean isPrimaryOrganizer;

    public final boolean isSponsoredEvent;

    public final int ticketingEnabledStatus;

    public final String zone;

    public final ArrayList<EventFilterAttribute> attributes;

    public final HashMap<String, Boolean> attributeValues;

    public final boolean isEvergreen;

    public final boolean isEhTicketing;

    public final ArrayList<EventZendeskTicketObject> faqs;

    public String discountPercentage;


    //DiscountPercentageText is given priority above discountPercentage
    public String discountPercentageText;

    public final boolean skipRequestToCall;

    public final String skipCallbackupPhone;

    public Event(String id, City city, String title, EventCategory category,
                 String description, ArrayList<String> tags, @Nullable String youtubeVideoId,
                 @Nullable String imgUrl, ArrayList<String> allImages, @Nullable String sourceUrl,
                 @Nullable String bookingUrl, @Nullable String bookingText,
                 int numViews, int numSaves, boolean ehRecommended,
                 float uberScore, List<Long> eventTimings,
                 @Nullable LatLng location, @Nullable String venue, @Nullable String locality,
                 @Nullable String address, boolean isCleanVenue,
                 List<String> performers,
                 String organizerName, String organizerPhone, String organizerWebsite,
                 String organizerEmail, String organizerLink, String organizerAccountName, ArrayList<EhPrices> ehPrices,
                 double minPrice, double maxPrice, @Nullable String currency, String priceName, String priceNote,
                 List<EventDescriptionSection> descriptionSections, ArrayList<MovieUserReviewObject> reviewObjects,
                 @Nullable String requestPerAttendeeData, @Nullable List<AdditionalTicketField> additionalTicketFieldList, List<EventSession> sessions, String sessionTitlePhrase, boolean isPrimaryOrganizer, boolean isSponsoredEvent, int ticketingEnabledStatus, String zone,
                 ArrayList<EventFilterAttribute> attributes, HashMap<String, Boolean> attributeValues, boolean isEvergreen, boolean isEhTicketing, ArrayList<EventZendeskTicketObject> faqs, String discountPercentage, String discountPercentageText, boolean skipRequestToCall, String skipCallbackupPhone) {
        this.id = id;
        this.city = city;
        this.title = title;
        this.category = category;

        this.description = description;
        this.tags = tags;
        this.youtubeVideoId = Utils.checkIfUnknown(youtubeVideoId);

        this.imgUrl = Utils.checkIfUnknown(imgUrl);
        this.allImages = allImages;
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
        this.organizerAccountName = Utils.checkIfUnknown(organizerAccountName);

        this.ehPrices = ehPrices;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.currency = Utils.checkIfUnknown(currency);
        this.priceName = priceName;
        this.priceNote = priceNote;

        this.descriptionSections = descriptionSections;
        this.reviewObjects = reviewObjects;

        this.requestPerAttendeeData = Utils.checkIfUnknown(requestPerAttendeeData);
        this.additionalTicketFieldList = additionalTicketFieldList;
        this.sessions = sessions;
        this.sessionTitlePhrase = sessionTitlePhrase;
        this.isPrimaryOrganizer = isPrimaryOrganizer;
        this.isSponsoredEvent = isSponsoredEvent;
        this.ticketingEnabledStatus = ticketingEnabledStatus;
        this.zone = zone;
        this.attributes = attributes;
        this.attributeValues = attributeValues;
        this.isEvergreen = isEvergreen;
        this.isEhTicketing = isEhTicketing;
        this.faqs = faqs;
        this.discountPercentage = discountPercentage;
        this.discountPercentageText = discountPercentageText;
        this.skipRequestToCall = skipRequestToCall;
        this.skipCallbackupPhone = skipCallbackupPhone;
    }

    public Event(Parcel in) {
        this.id = in.readString();
        String cityName = in.readString();
        this.city = Utils.checkIfStringEmpty(cityName) ? null : City.parseCity(cityName);
        this.title = in.readString();
        this.category = EventCategory.parseCategory(in.readString());

        this.description = in.readString();
        this.tags = new ArrayList<>();
        in.readStringList(tags);
        this.youtubeVideoId = Utils.checkIfUnknown(in.readString());

        this.imgUrl = Utils.checkIfUnknown(in.readString());
        allImages = new ArrayList<>();
        in.readStringList(allImages);
        this.sourceUrl = Utils.checkIfUnknown(in.readString());
        this.bookingUrl = Utils.checkIfUnknown(in.readString());
        this.bookingText = Utils.checkIfUnknown(in.readString());

        this.numViews = in.readInt();
        this.numSaves = in.readInt();
        this.ehRecommended = in.readInt() == 1;
        this.uberScore = in.readFloat();

        this.eventTimings = new ArrayList<>();
        in.readList(eventTimings, Long.class.getClassLoader());
        LatLng sLocation = (LatLng) in.readParcelable(LatLng.class.getClassLoader());
        this.location = sLocation != null && city.cityBounds.contains(sLocation) ? sLocation : null;
        this.venue = Utils.checkIfUnknown(in.readString());
        this.locality = Utils.checkIfUnknown(in.readString());
        this.address = Utils.checkIfUnknown(in.readString());
        this.isCleanVenue = venue != null && in.readInt() == 1;

        this.performers = new ArrayList<>();
        in.readStringList(performers);

        this.organizerName = Utils.checkIfUnknown(in.readString());
        this.organizerPhone = Utils.checkIfUnknown(in.readString());
        this.organizerWebsite = Utils.checkIfUnknown(in.readString());
        this.organizerEmail = Utils.checkIfUnknown(in.readString());
        this.organizerLink = Utils.checkIfUnknown(in.readString());
        this.organizerAccountName = Utils.checkIfUnknown(in.readString());

        ehPrices = new ArrayList<>();
        in.readTypedList(ehPrices, EhPrices.CREATOR);
        this.minPrice = in.readDouble();
        this.maxPrice = in.readDouble();
        this.currency = Utils.checkIfUnknown(in.readString());
        this.priceName = in.readString();
        this.priceNote = in.readString();
        descriptionSections = new ArrayList<>();
        in.readTypedList(descriptionSections, EventDescriptionSection.CREATOR);
        // this.descriptionSections = in.createTypedArray(EventDescriptionSection.CREATOR);
        reviewObjects = new ArrayList<>();
        in.readTypedList(reviewObjects, MovieUserReviewObject.CREATOR);
        this.requestPerAttendeeData = in.readString();
        additionalTicketFieldList = new ArrayList<>();
        in.readTypedList(additionalTicketFieldList, AdditionalTicketField.CREATOR);
        sessions = new ArrayList<>();
        in.readTypedList(sessions, EventSession.CREATOR);
        sessionTitlePhrase = in.readString();
        isPrimaryOrganizer = in.readInt() == 1;
        isSponsoredEvent = in.readInt() == 1;
        ticketingEnabledStatus = in.readInt();
        zone = in.readString();
        attributes = new ArrayList<>();
        in.readTypedList(attributes, EventFilterAttribute.CREATOR);
        attributeValues = new HashMap<>();
        in.readMap(attributeValues, Boolean.class.getClassLoader());
        isEvergreen = in.readInt() == 1;
        isEhTicketing = in.readInt() == 1;
        faqs = new ArrayList<>();
        in.readTypedList(faqs, EventZendeskTicketObject.CREATOR);
        discountPercentage = in.readString();
        discountPercentageText = in.readString();
        skipRequestToCall = in.readInt() == 1;
        skipCallbackupPhone = in.readString();
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
        dest.writeStringList(allImages);
        dest.writeString(emptyIfNull(sourceUrl));
        dest.writeString(emptyIfNull(bookingUrl));
        dest.writeString(emptyIfNull(bookingText));
        dest.writeInt(numViews);
        dest.writeInt(numSaves);
        dest.writeInt(ehRecommended ? 1 : 0);
        dest.writeFloat(uberScore);
        dest.writeList(eventTimings);
        dest.writeParcelable(location == null ? new LatLng(0, 0) : location, flags);
        dest.writeString(emptyIfNull(venue));
        dest.writeString(emptyIfNull(locality));
        dest.writeString(emptyIfNull(address));
        dest.writeInt(isCleanVenue ? 1 : 0);
        dest.writeStringList(performers);
        dest.writeString(emptyIfNull(organizerName));
        dest.writeString(emptyIfNull(organizerPhone));
        dest.writeString(emptyIfNull(organizerWebsite));
        dest.writeString(emptyIfNull(organizerEmail));
        dest.writeString(emptyIfNull(organizerLink));
        dest.writeString(emptyIfNull(organizerAccountName));
        dest.writeTypedList(ehPrices);
        dest.writeDouble(minPrice);
        dest.writeDouble(maxPrice);
        dest.writeString(emptyIfNull(currency));
        dest.writeString(priceName);
        dest.writeString(priceNote);
        dest.writeTypedList(descriptionSections);
        dest.writeTypedList(reviewObjects);
        dest.writeString(requestPerAttendeeData);
        dest.writeTypedList(additionalTicketFieldList);
        dest.writeTypedList(sessions);
        dest.writeString(sessionTitlePhrase);
        dest.writeInt(isPrimaryOrganizer ? 1 : 0);
        dest.writeInt(isSponsoredEvent ? 1 : 0);
        dest.writeInt(ticketingEnabledStatus);
        dest.writeString(zone);
        dest.writeTypedList(attributes);
        dest.writeMap(attributeValues);
        dest.writeInt(isEvergreen ? 1 : 0);
        dest.writeInt(isEhTicketing ? 1 : 0);
        dest.writeTypedList(faqs);
        dest.writeString(discountPercentage);
        dest.writeString(discountPercentageText);
        dest.writeInt(skipRequestToCall ? 1 : 0);
        dest.writeString(skipCallbackupPhone);
    }

    public Uri getEventDetailsURI() {
        return EventsHighEndpoints.getEventDetailsURI(this);
    }

    public Uri getEventShareURI() {
        return getEventShareURI(null);
    }

    public boolean isRequestPerAttendeeData() {
        if (Utils.checkIfStringEmpty(requestPerAttendeeData))
            return true;
        else
            return requestPerAttendeeData.equals("on");
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

    public double getMinPrice() {
        if (!ehPrices.isEmpty()) {
            return ehPrices.get(0).value;
        }

        if (minPrice > -0.1) {
            return minPrice;
        }

        return Integer.MAX_VALUE;
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
        City city = null;
        try {
            city = City.valueOf(eventJson.getString("city").toUpperCase());


            JSONObject mashup = eventJson.optJSONObject("mashup");
            String source_url = eventJson.optString("source_url");
            String booking_url = mashup == null ? null : mashup.optString("booking_url");
            String booking_text = eventJson.optString("booking_text");
            String img_url = eventJson.optString("img_url");
            if ((source_url != null && source_url.toLowerCase().contains("eventviva")) ||
                    (img_url != null && img_url.endsWith("missing.png"))) {
                img_url = null;
            }

            ArrayList<String> allImages = new ArrayList<>();
            String allImageUrls = eventJson.optString("all_images");
            if (allImageUrls != null && allImageUrls.length() > 0) {
                for (String url : allImageUrls.split(",")) {
                    allImages.add(url);
                }
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
            if (city != null && !city.cityBounds.contains(new LatLng(lat, lon)) && localityJson != null) {
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

                tagsList.add(Utils.capitalize(tag));

                if (category == EventCategory.OTHER) {
                    EventCategory tagCategory = EventCategory.parseCategory(tag);
                    if (tagCategory != null) {
                        category = tagCategory;
                    }
                }
            }
            if (title.equalsIgnoreCase("Nritya Shakti Tour 2016-An intensive workshop by Shakti Mohan")) {
                tagsList.hashCode();
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

          /*  long[] eventTimingsArr = new long[eventTimings.size()];
            int i = 0;
            for (Long eventTime : eventTimings) {
                eventTimingsArr[i] = eventTime;
                i++;
            }*/

            // Performers
            JSONArray participantsInfo = eventJson.optJSONArray("participants");
            List<String> performers = new ArrayList<>(participantsInfo == null ? 0 : participantsInfo.length());
            if (participantsInfo != null) {
                for (int i = 0; i < participantsInfo.length(); i++) {
                    String performer = participantsInfo.getJSONObject(i).optString("name");
                    if (performer != null) {
                        performers.add(performer);
                    }
                }
            }

            // Organizer Info.

            String organizerName = null;
            String organizerPhone = null;
            String organizerWebsite = null;
            String organizerEmail = null;
            String organizerLink = null;
            if (eventJson.has("attributes") && eventJson.optJSONObject("attributes").has("organizer_info")) {
                JSONObject organizerInfo = eventJson.optJSONObject("attributes").optJSONObject("organizer_info");
                organizerName = organizerInfo.optString("name");
                organizerPhone = organizerInfo.optString("phone");
                organizerEmail = organizerInfo.optString("email");
                organizerWebsite = organizerInfo.optString("website");

            }

            String organizerAccountName = eventJson.optString("organizer_account_name");

            boolean skipRequestToCall = eventJson.optBoolean("skip_request_to_call");
            String skipCallBackupPhone = eventJson.optString("skip_call_backup_phone");

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
                    Collections.sort(ehOccurences);
                    ehPriceName = ehPrice.optString("name", "");
                    ehPriceNote = ehPrice.optString("note", "");
                    double discountValue = ehPrice.optDouble("discount_value", -1);
                    double value = ehPrice.optDouble("value", -1);
                    String startVaild = ehPrice.getString("validity_start");
                    String endValid = ehPrice.getString("validity_end");
                    if (Utils.checkIfStringEmpty(startVaild)) startVaild = "1907-01-01 00:00:00.0";
                    if (Utils.checkIfStringEmpty(endValid)) endValid = "2099-01-01 00:00:00.0";
                    long validityStart = DateTimeUtils.parseOfferTime(startVaild);
                    long validityEnd = DateTimeUtils.parseOfferTime(endValid);
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

                    long timenow = System.currentTimeMillis();
                    boolean isMulti = ehPrice.optInt("is_multi") == 0 ? false : true;
                    if (validityStart <= timenow && validityEnd > timenow)
                        ehPrices.add(EhPrices.createObject(minPrice, maxPrice, ehPriceName, ehPriceNote, currency, value, discountValue, ehOccurences, 0,
                                validityStart, validityEnd, isMulti));
                }

            }

            Collections.sort(ehPrices);

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
            } else {
                minPrice = ehPrices.get(0).min;
                maxPrice = ehPrices.get(ehPrices.size() - 1).max;
            }

            //User Reviews

            ArrayList<MovieUserReviewObject> reviews = new ArrayList<>();
            if (eventJson.has("reviews")) {
                JSONArray reviewsArray = eventJson.getJSONArray("reviews");
                if (reviewsArray != null) {
                    for (int l = 0; l < reviewsArray.length(); l++) {
                        MovieUserReviewObject obj = new MovieUserReviewObject(reviewsArray.getJSONObject(l));
                        if (obj.getReviewState() == null || obj.getReviewState().equalsIgnoreCase("published")) {
                            reviews.add(obj);
                        }
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


            //Attributes
            List<AdditionalTicketField> additionalTicketFieldList = new ArrayList<>();
            String requestPerAttendeeData = eventJson.optString("request_per_attendee_data", "");
            if (eventJson.has("additional_fields")) {
                JSONArray additionalFieldsJsonArray = eventJson.getJSONArray("additional_fields");
                for (int j = 0; j < additionalFieldsJsonArray.length(); j++) {
                    AdditionalTicketField additionalTicketField = AdditionalTicketField.fromJsonObject(additionalFieldsJsonArray.getJSONObject(j));
                    additionalTicketFieldList.add(additionalTicketField);
                }
            }

            List<EventSession> sessions = new ArrayList<>();

            JSONArray sessionsJsonArray = eventJson.optJSONArray("sessions");
            if (sessionsJsonArray != null) {
                for (int i = 0; i < sessionsJsonArray.length(); i++) {
                    sessions.add(new EventSession(sessionsJsonArray.getJSONObject(i)));
                }
            }
            String sessionTitlePhrase = null;
            if (eventJson.has("session_title_phrase")) {
                sessionTitlePhrase = Utils.checkIfUnknown(eventJson.optString("session_title_phrase"));
            }

            boolean isPrimaryOrganizer = false;
            if (eventJson.has("is_primary_organizer")) {
                isPrimaryOrganizer = eventJson.getBoolean("is_primary_organizer");
            }

            boolean isSponsoredEvent = false;
            if (eventJson.has("sponsor_info")) {
                isSponsoredEvent = eventJson.getJSONObject("sponsor_info").getString("is_sponsored_event").equalsIgnoreCase("on");
            }

            String zone = eventJson.optString("zone");
            if (zone == null || venue == null) {
                System.out.println("Event Id :: " + id);
            }
            if (zone.equalsIgnoreCase("unknown") && venue.equalsIgnoreCase("outside " + city.name())) {
                zone = "Outside " + (city.name());
            }

            String discountPercentage = null;
            String discountPercentageText = null;
            ArrayList<EventFilterAttribute> attributes = new ArrayList<>();
            HashMap<String, Boolean> attributeValues = new HashMap<>();
            if (eventJson.has("attributes")) {
                if (eventJson.optJSONObject("attributes").has("include_value_attributes")) {
                    if (eventJson.optJSONObject("attributes").optJSONObject("include_value_attributes").has("include_values")) {
                        JSONArray jsonArray = eventJson.optJSONObject("attributes").optJSONObject("include_value_attributes").optJSONArray("include_values");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String name = jsonObject.optString("name");
                            boolean value = jsonObject.optBoolean("is_included");
                            attributes.add(new EventFilterAttribute(name, value));
                            attributeValues.put(name, value);
                        }

                        //attributes = EventFilterAttribute.getAttributes(jsonArray);
                    }
                }
                if (eventJson.optJSONObject("attributes").has("key_value_attributes") && eventJson.optJSONObject("attributes").optJSONObject("key_value_attributes").has("key_values")) {
                    JSONArray keyValuePairArray = eventJson.optJSONObject("attributes").optJSONObject("key_value_attributes").optJSONArray("key_values");
                    for (int i = 0; i < keyValuePairArray.length(); i++) {
                        JSONObject obj = keyValuePairArray.optJSONObject(i);
                        if (obj.getString("key").equalsIgnoreCase(CONSTANT_DISCOUNT_PERCENTAGE_TEXT)) {
                            discountPercentageText = obj.getString("value");
                        }
                        if (obj.getString("key").equalsIgnoreCase(CONSTANT_DISCOUNT_PERCENTAGE)) {
                            discountPercentage = obj.getString("value");
                        }

                    }
                }

            }

            boolean isEvergreen = eventJson.optBoolean("evergreen");
            boolean isEhTicketing = eventJson.optBoolean("is_eh_ticketing");
            int ticketingEnabledStatus = eventJson.optInt("ticketing_enabled_status");

            ArrayList<EventZendeskTicketObject> faqs = new ArrayList<>();
            if (eventJson.has("zendesk_tickets")) {
                JSONArray zendeskTickets = eventJson.optJSONArray("zendesk_tickets");
                for (int i = 0; i < zendeskTickets.length(); i++) {
                    faqs.add(EventZendeskTicketObject.parseZendeskObj(zendeskTickets.getJSONObject(i)));
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
                    allImages,
                    source_url,
                    booking_url,
                    booking_text,

                    num_views,
                    num_saves,
                    eh_recommends,
                    uberScore,

                    eventTimings,

                    new LatLng(lat, lon),
                    venue,
                    locality,
                    address,
                    isCleanVenue,
                    performers,
                    organizerName,
                    organizerPhone,
                    organizerWebsite,
                    organizerEmail,
                    organizerLink,
                    organizerAccountName,
                    ehPrices,
                    minPrice,
                    maxPrice,
                    currency,
                    ehPriceName,
                    ehPriceNote,
                    descriptionSections,
                    reviews,
                    requestPerAttendeeData,
                    additionalTicketFieldList,
                    sessions,
                    sessionTitlePhrase,
                    isPrimaryOrganizer,
                    isSponsoredEvent,
                    ticketingEnabledStatus,
                    zone, attributes,
                    attributeValues,
                    isEvergreen,
                    isEhTicketing,
                    faqs,
                    discountPercentage,
                    discountPercentageText,
                    skipRequestToCall,
                    skipCallBackupPhone
            );
        } catch (IllegalArgumentException e) {
            Log.i("Exception caught", e.getMessage());
            Crashlytics.logException(e);
            return null;
        }
    }

    public static List<Event> fromJSON(JSONArray jsonArray, boolean includeWithoutLocation, OnPartialDataLoadingComplete listener) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                Event event = fromJSON(jsonArray.getJSONObject(i));
                if (event != null && (includeWithoutLocation || event.location != null)) {
                    events.add(event);
                }
                if (listener != null) {
                    if (i == 19) {
                        listener.onPartialLoadingComplete(events);
                        events = new ArrayList<>();
                    } else if (i == jsonArray.length() - 1) {
                        if (events.size() > 20) {
                            listener.onFullDataLoadingComplete(events.subList(20, events.size()));
                        } else {
                            listener.onFullDataLoadingComplete(events);
                        }
                    }
                }
            } catch (JSONException | ParseException e) {
                Crashlytics.getInstance().core.logException(e);
            }
        }
        return events;
    }


    public static List<Event> fromJSON(Context context, JSONArray jsonArray, boolean includeWithoutLocation, boolean isForSavingAction) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                Event event = fromJSON(jsonArray.getJSONObject(i));
                if (event != null && (includeWithoutLocation || event.location != null)) {
                    if (isForSavingAction)
                        ((BaseContextActivity) context).recordEventMark(event, EventsMarkerManager.EventMark.FAVOURITE, isForSavingAction);
                    events.add(event);

                }
            } catch (JSONException | ParseException e) {
                Crashlytics.getInstance().core.logException(e);
            }
        }
        return events;
    }


    public static List<Event> parseUpcomingEvents(JSONObject eventsJSON,
                                                  boolean includeWithoutLocation, OnPartialDataLoadingComplete listener) throws JSONException {


        List<Event> allEvents = new ArrayList<>();

        if (eventsJSON.has("evergreen_events")) {
            JSONArray evergreenEvents = eventsJSON.getJSONArray("evergreen_events");
            if (eventsJSON.has("upcoming_events")) {
                allEvents.addAll(fromJSON(evergreenEvents, includeWithoutLocation, null));
            } else {
                allEvents.addAll(fromJSON(evergreenEvents, includeWithoutLocation, listener));
            }
        }
        if (eventsJSON.has("upcoming_events")) {
            JSONArray upcomingEvents = eventsJSON.getJSONArray("upcoming_events");
            allEvents.addAll(fromJSON(upcomingEvents, includeWithoutLocation, listener));
        }
        return allEvents;
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

    public Intent getShowDirectionsOnMapIntent(Context context) {
        String query = getMapQuery();
        if (query == null || !isPackageExisted(context, "com.google.android.apps.maps")) {
            return null;
        }

        // From https://developers.google.com/maps/documentation/android/intents
        Uri locationUri = Uri.parse("google.navigation:q=" + query);
        Intent mapIntent = new Intent(android.content.Intent.ACTION_VIEW, locationUri);
        PackageManager manager = context.getPackageManager();
        mapIntent.setPackage("com.google.android.apps.maps");
        return mapIntent;
    }

    public boolean isPackageExisted(Context context, String targetPackage) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }


    public interface OnPartialDataLoadingComplete {
        void onPartialLoadingComplete(List<Event> events);

        void onFullDataLoadingComplete(List<Event> events);
    }

}
