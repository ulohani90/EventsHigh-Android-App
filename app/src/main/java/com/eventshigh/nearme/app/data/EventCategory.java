package com.eventshigh.nearme.app.data;

import android.support.annotation.Nullable;
import android.util.Log;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Various Category for Events. The category defines the event type and has associated Icon.
 * This Icon is used in event marker on Map or on Event Info card.
 */
public enum EventCategory {
    TODAY,
    MOVIES,
    NIGHTLIFE("Parties & Nightlife"),
    LIVE_PERFORMANCES("Live Shows"),
    OUTDOORS,
    HEALTH_WELLNESS("Health & Wellness"),
    KIDS_ENTERTAINMENT("Kids"),
    SPORTS,
    WORKSHOPS,
    TECH("Technology"),
    ART("Arts & Culture"),
    FOOD,
    COMEDY,
    DANCE,
    DJ,
    EDUCATION,
    ENVIRONMENT,
    FASHION,
    FILM,
    LITERATURE,
    MUSIC,
    PHOTOGRAPHY,
    SHOPPING,
    SOCIAL_CAUSES,
    SPIRITUAL,
    THEATRE,
    EDITOR_PICKS("Editor's Picks"),
    FREE_EVENTS,
    OTHER;

    private static final String LOG_TAG = EventCategory.class.getSimpleName();
    public final String categoryName;

    EventCategory(@Nullable String categoryName) {
        this.categoryName = categoryName == null ? Utils.capitalize(name().replace('_', ' ')) : categoryName;
    }

    EventCategory() {
        this(null);
    }

    /**
     * Get an Icon associated with this Category.
     *
     * @return an BitmapDescriptor for Icon.
     */
    public BitmapDescriptor icon() {
        BitmapDescriptor icon = CATEGORY_ICONS.get(this);
        if (icon == null && this != null) {
            icon = BitmapDescriptorFactory.fromResource(getIconResourceId());
            CATEGORY_ICONS.put(this, icon);
        }
        return icon;
    }

    /**
     * Get the recommendation icon (used when showing recommended events on the map) associated
     * with this category.
     *
     * @return an BitmapDescriptor for Icon.
     */
    public BitmapDescriptor recommendationIcon() {
        BitmapDescriptor icon = CATEGORY_RECOMMENDATION_ICONS.get(this);
        if (icon == null) {
            icon = BitmapDescriptorFactory.fromResource(getHighlightedIconResourceId());
            CATEGORY_RECOMMENDATION_ICONS.put(this, icon);
        }
        return icon;
    }

    public int getIconResourceId() {
        int resId = R.drawable.icon_other;

        try {
            resId = R.drawable.class.getField("icon_" + toString().toLowerCase()).getInt(null);
        } catch (IllegalAccessException e) {
            // Ignore
        } catch (NoSuchFieldException e) {
            // Ignore
            Log.d(LOG_TAG, "no icon: " + name(), e);
        }

        return resId;
    }


    public int getInterestIconResourceId() {
        int resId = R.drawable.icon_kids_entertainment_int;
        try {
            resId = R.drawable.class.getField("icon_" + toString().toLowerCase() + "_int").getInt(null);
        } catch (IllegalAccessException e) {
            // Ignore
        } catch (NoSuchFieldException e) {
            // Ignore
            Log.d(LOG_TAG, "no icon: " + name(), e);
        }

        return resId;
    }


    public int getHighlightedIconResourceId() {
        int resId = R.drawable.icon_other_rec;
        try {
            resId = R.drawable.class.getField("icon_" + toString().toLowerCase() + "_rec")
                    .getInt(null);
        } catch (IllegalAccessException e) {
            // Ignore
        } catch (NoSuchFieldException e) {
            // Ignore
            Log.d(LOG_TAG, "no icon: " + name(), e);
        }

        return resId;
    }

    public static String toCategoryParsableString(String tag) {
        return tag.toUpperCase().replaceAll(" ", "_").replaceAll("&_", "").replaceAll("'", "").replaceAll("\\(", "_").replaceAll("\\)", "").replaceAll(",", "");
    }

    public static
    @Nullable
    EventCategory getCategoryFromCategoryParsableString(String tagU) {
        try {
            return valueOf(tagU);
        } catch (IllegalArgumentException e) {
            // Ignore. Unsupported category.
        }
        return null;
    }


    public static boolean isACategory(String categoryName) {
        for (EventCategory category : EventCategory.values()) {
            if (category.categoryName.equalsIgnoreCase(categoryName))
                return true;
        }
        return false;
    }


    public static
    @Nullable
    EventCategory parseCategory(String tag) {
        return getCategoryFromCategoryParsableString(toCategoryParsableString(tag));
    }

    public static BitmapDescriptor circleIcon() {
        if (CIRCLE_ICON == null) {
            CIRCLE_ICON = BitmapDescriptorFactory.fromResource(R.drawable.icon_dot);
        }

        return CIRCLE_ICON;
    }

    private static BitmapDescriptor CIRCLE_ICON;
    private static final Map<EventCategory, BitmapDescriptor> CATEGORY_ICONS = Utils.getMap();
    private static final Map<EventCategory, BitmapDescriptor> CATEGORY_RECOMMENDATION_ICONS =
            new HashMap<>();
}
