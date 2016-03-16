package com.eventshigh.nearme.app.data;

import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.utils.Utils;

/**
 * Various Category for Events. The category defines the event type and has associated Icon.
 * This Icon is used in event marker on Map or on Event Info card.
 */
public enum EventCategory {
    ART,
    COMEDY,
    DANCE,
    DJ,
    EDUCATION,
    ENVIRONMENT,
    FASHION,
    FILM,
    FOOD,
    HEALTH_WELLNESS("Health & Wellness"),
    KIDS_ENTERTAINMENT,
    LITERATURE,
    MUSIC,
    OUTDOORS,
    PARTIES,
    PHOTOGRAPHY,
    SHOPPING,
    SOCIAL_CAUSES,
    SPIRITUAL,
    SPORTS,
    TECH,
    THEATRE,
    OTHER;

    public final String categoryName;

     EventCategory(@Nullable String categoryName) {
        this.categoryName = categoryName == null ? Utils.capitalize(name().replace('_', ' ')) : categoryName;
    }

     EventCategory() {
        this(null);
    }

    public static String toCategoryParsableString(String tag) {
        return tag.toUpperCase().replaceAll(" ", "_").replaceAll("&_", "").replaceAll("'", "");
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
