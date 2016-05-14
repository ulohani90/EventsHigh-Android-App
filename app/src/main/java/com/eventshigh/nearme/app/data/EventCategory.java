package com.eventshigh.nearme.app.data;

import android.support.annotation.Nullable;
import android.util.Log;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.Utils;

/**
 * Various Category for Events. The category defines the event type and has associated Icon.
 * This Icon is used in event marker on Map or on Event Info card.
 */
public enum EventCategory {
    ART("Arts & Culture"),
    COMEDY,
    DANCE,
    DJ,
    EDUCATION,
    ENVIRONMENT,
    FASHION,
    FILM,
    FOOD,
    HEALTH_WELLNESS("Health & Wellness"),
    KIDS_ENTERTAINMENT("Kids"),
    LITERATURE("TLS(Technology, Literature & Society)"),
    MUSIC,
    OUTDOORS,
    NIGHTLIFE("Parties & Nightlife"),
    PHOTOGRAPHY,
    SHOPPING,
    SOCIAL_CAUSES,
    SPIRITUAL,
    SPORTS,
    TECH,
    THEATRE,
    WORKSHOP,
    LIVE_PERFORMANCES("Live Performances & Shows"),
    EDITOR_PICKS("Editor's Picks"),
    FREE_EVENTS,
    OTHER,
    MOVIES;

    public final String categoryName;

    private static final String LOG_TAG = EventCategory.class.getSimpleName();

     EventCategory(@Nullable String categoryName) {
        this.categoryName = categoryName == null ? Utils.capitalize(name().replace('_', ' ')) : categoryName;
    }

     EventCategory() {
        this(null);
    }


    public int getInterestIconResourceId() {
        int resId = R.drawable.icon_kids_entertainment_int;
        try {
            resId = R.drawable.class.getField("icon_" + toString().toLowerCase()+"_int").getInt(null);
        } catch (IllegalAccessException e) {
            // Ignore
        } catch (NoSuchFieldException e) {
            // Ignore
            Log.d(LOG_TAG, "no icon: " + name(), e);
        }

        return resId;
    }






    public static String toCategoryParsableString(String tag) {
        return tag.toUpperCase().replaceAll(" ", "_").replaceAll("&_", "").replaceAll("'", "").replaceAll("\\(","_").replaceAll("\\)","").replaceAll(",","");
    }

    public static @Nullable EventCategory getCategoryFromCategoryParsableString(String tagU) {
        try {
            return valueOf(tagU);
        } catch (IllegalArgumentException e) {
            // Ignore. Unsupported category.
        }
        return  null;
    }

    public static @Nullable EventCategory parseCategory(String tag) {
        return getCategoryFromCategoryParsableString(toCategoryParsableString(tag));
    }
}
