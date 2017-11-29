package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.data.stream.AdditionalTicketField;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by umesh on 18/08/17.
 */

public class EventInfoObject implements Parcelable {

    public final String id;
    public final String city;
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

    public final ArrayList<MovieUserReviewObject> reviewObjects;

    @Nullable
    public final String requestPerAttendeeData;
    @Nullable

    public final String sessionTitlePhrase;

    public final boolean isPrimaryOrganizer;

    public final boolean isSponsoredEvent;

    public final int ticketingEnabledStatus;

    public final String zone;

    public final boolean isEvergreen;

    public final boolean isEhTicketing;

    public String discountPercentage;


    //DiscountPercentageText is given priority above discountPercentage
    public String discountPercentageText;

    public final boolean skipRequestToCall;

    public final String skipCallbackupPhone;

    public final String destination;

    public final String timezone;

    public final String config;

    public EventInfoObject(Event event) {

        this.id = event.id;
        this.city = event.city;
        this.title = event.title;
        this.category = event.category;


        this.description = event.description;
        this.tags = event.tags;
        this.youtubeVideoId = Utils.checkIfUnknown(event.youtubeVideoId);

        this.imgUrl = Utils.checkIfUnknown(event.imgUrl);
        this.allImages = event.allImages;
        this.sourceUrl = Utils.checkIfUnknown(event.sourceUrl);
        this.bookingUrl = Utils.checkIfUnknown(event.bookingUrl);
        this.bookingText = Utils.checkIfUnknown(event.bookingText);

        this.numViews = event.numViews;
        this.numSaves = event.numSaves;
        this.ehRecommended = event.ehRecommended;
        this.uberScore = event.uberScore;

        this.eventTimings = event.eventTimings;

        this.location = event.location != null ? event.location : null;
        this.venue = Utils.checkIfUnknown(event.venue);
        this.locality = Utils.checkIfUnknown(event.locality);
        this.address = Utils.checkIfUnknown(event.address);
        this.isCleanVenue = event.venue != null && event.isCleanVenue;

        this.performers = event.performers;

        this.organizerName = Utils.checkIfUnknown(event.organizerName);
        this.organizerPhone = Utils.checkIfUnknown(event.organizerPhone);
        this.organizerWebsite = Utils.checkIfUnknown(event.organizerWebsite);
        this.organizerEmail = Utils.checkIfUnknown(event.organizerEmail);
        this.organizerLink = Utils.checkIfUnknown(event.organizerLink);
        this.organizerAccountName = Utils.checkIfUnknown(event.organizerAccountName);

        this.ehPrices = event.ehPrices;
        this.minPrice = event.minPrice;
        this.maxPrice = event.maxPrice;
        this.currency = Utils.checkIfUnknown(event.currency);
        this.priceName = event.priceName;
        this.priceNote = event.priceNote;


        this.reviewObjects = event.reviewObjects;

        this.requestPerAttendeeData = Utils.checkIfUnknown(event.requestPerAttendeeData);

        this.sessionTitlePhrase = event.sessionTitlePhrase;
        this.isPrimaryOrganizer = event.isPrimaryOrganizer;
        this.isSponsoredEvent = event.isSponsoredEvent;
        this.ticketingEnabledStatus = event.ticketingEnabledStatus;
        this.zone = event.zone;

        this.isEvergreen = event.isEvergreen;
        this.isEhTicketing = event.isEhTicketing;

        this.discountPercentage = event.discountPercentage;
        this.discountPercentageText = event.discountPercentageText;
        this.skipRequestToCall = event.skipRequestToCall;
        this.skipCallbackupPhone = event.skipCallbackupPhone;
        this.destination = event.destination;
        this.timezone = event.timezone;
        this.config = event.config;
    }

    protected EventInfoObject(Parcel in) {
        id = in.readString();
        String cityName = in.readString();
        this.city = Utils.checkIfStringEmpty(cityName) ? null : cityName;
        title = in.readString();
        this.category = EventCategory.parseCategory(in.readString());
        description = in.readString();
        tags = in.createStringArrayList();
        youtubeVideoId = in.readString();
        imgUrl = in.readString();
        allImages = in.createStringArrayList();
        sourceUrl = in.readString();
        bookingUrl = in.readString();
        bookingText = in.readString();
        numViews = in.readInt();
        numSaves = in.readInt();
        ehRecommended = in.readByte() != 0;
        uberScore = in.readFloat();

        this.eventTimings = new ArrayList<>();
        in.readList(eventTimings, Long.class.getClassLoader());

        location = in.readParcelable(LatLng.class.getClassLoader());
        venue = in.readString();
        locality = in.readString();
        address = in.readString();
        isCleanVenue = in.readByte() != 0;
        performers = in.createStringArrayList();
        organizerName = in.readString();
        organizerPhone = in.readString();
        organizerWebsite = in.readString();
        organizerEmail = in.readString();
        organizerLink = in.readString();
        organizerAccountName = in.readString();
        minPrice = in.readDouble();
        maxPrice = in.readDouble();
        currency = in.readString();
        priceName = in.readString();
        priceNote = in.readString();
        ehPrices = in.createTypedArrayList(EhPrices.CREATOR);
        reviewObjects = in.createTypedArrayList(MovieUserReviewObject.CREATOR);
        requestPerAttendeeData = in.readString();
        sessionTitlePhrase = in.readString();
        isPrimaryOrganizer = in.readByte() != 0;
        isSponsoredEvent = in.readByte() != 0;
        ticketingEnabledStatus = in.readInt();
        zone = in.readString();
        isEvergreen = in.readByte() != 0;
        isEhTicketing = in.readByte() != 0;
        discountPercentage = in.readString();
        discountPercentageText = in.readString();
        skipRequestToCall = in.readByte() != 0;
        skipCallbackupPhone = in.readString();
        destination = in.readString();
        timezone = in.readString();
        config = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(city.toString());
        dest.writeString(title);
        dest.writeString(category.toString());
        dest.writeString(description);
        dest.writeStringList(tags);
        dest.writeString(youtubeVideoId);
        dest.writeString(imgUrl);
        dest.writeStringList(allImages);
        dest.writeString(sourceUrl);
        dest.writeString(bookingUrl);
        dest.writeString(bookingText);
        dest.writeInt(numViews);
        dest.writeInt(numSaves);
        dest.writeByte((byte) (ehRecommended ? 1 : 0));
        dest.writeFloat(uberScore);
        dest.writeList(eventTimings);
        dest.writeParcelable(location, flags);
        dest.writeString(venue);
        dest.writeString(locality);
        dest.writeString(address);
        dest.writeByte((byte) (isCleanVenue ? 1 : 0));
        dest.writeStringList(performers);
        dest.writeString(organizerName);
        dest.writeString(organizerPhone);
        dest.writeString(organizerWebsite);
        dest.writeString(organizerEmail);
        dest.writeString(organizerLink);
        dest.writeString(organizerAccountName);
        dest.writeDouble(minPrice);
        dest.writeDouble(maxPrice);
        dest.writeString(currency);
        dest.writeString(priceName);
        dest.writeString(priceNote);
        dest.writeTypedList(ehPrices);
        dest.writeTypedList(reviewObjects);
        dest.writeString(requestPerAttendeeData);
        dest.writeString(sessionTitlePhrase);
        dest.writeByte((byte) (isPrimaryOrganizer ? 1 : 0));
        dest.writeByte((byte) (isSponsoredEvent ? 1 : 0));
        dest.writeInt(ticketingEnabledStatus);
        dest.writeString(zone);
        dest.writeByte((byte) (isEvergreen ? 1 : 0));
        dest.writeByte((byte) (isEhTicketing ? 1 : 0));
        dest.writeString(discountPercentage);
        dest.writeString(discountPercentageText);
        dest.writeByte((byte) (skipRequestToCall ? 1 : 0));
        dest.writeString(skipCallbackupPhone);
        dest.writeString(destination);
        dest.writeString(timezone);
        dest.writeString(config);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<EventInfoObject> CREATOR = new Creator<EventInfoObject>() {
        @Override
        public EventInfoObject createFromParcel(Parcel in) {
            return new EventInfoObject(in);
        }

        @Override
        public EventInfoObject[] newArray(int size) {
            return new EventInfoObject[size];
        }
    };

    public Uri getEventShareURI(@Nullable String src) {
        return EventsHighEndpoints.getEventShareURI(this, src);
    }

    public Uri getEventShareURI() {
        return getEventShareURI(null);
    }

    public String getFullAddress() {
        return (venue == null ? "" : venue + " ") + (address == null ? "" : address).trim();
    }

    public String getShortAddress() {
        String shortAddress = (venue == null ? "" : venue + " ") + (locality == null ? "" : "(" + locality + ")").trim();
        return shortAddress.isEmpty() ? Utils.capitalize(city) : shortAddress;
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

    public Uri getEventDetailsURI() {
        return EventsHighEndpoints.getEventDetailsURI(this);
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

    public boolean isPackageExisted(Context context, String targetPackage) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }
}
