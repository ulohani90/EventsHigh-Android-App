package com.eventshigh.nearme.app.data;

import com.eventshigh.nearme.app.R;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.HashMap;
import java.util.Map;

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
    HEALTH_WELLNESS,
    KIDS,
    LITERATURE,
    MUSIC,
    OUTDOORS,
    PARTIES,
    PHOTOGRAPHY,
    SOCIAL_CAUSES,
    SPIRITUAL,
    SPORTS,
    TECH,
    THEATRE,
    OTHER;

    /**
     * Get an Icon associated with this Category.
     *
     * @return an BitmapDescriptor for Icon.
     */
    public BitmapDescriptor icon() {
        BitmapDescriptor icon = CATEGORY_ICONS.get(this);
        if (icon == null) {
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
        } catch (IllegalAccessException| NoSuchFieldException e) {
            // Ignore
        }

        return resId;
    }

    public int getHighlightedIconResourceId() {
        int resId = R.drawable.icon_other_rec;
        try {
            resId = R.drawable.class.getField("icon_" + toString().toLowerCase() + "_rec")
                    .getInt(null);
        } catch (IllegalAccessException| NoSuchFieldException e) {
            // Ignore
        }

        return resId;
    }

    public static BitmapDescriptor circleIcon() {
        if (CIRCLE_ICON == null) {
            CIRCLE_ICON = BitmapDescriptorFactory.fromResource(R.drawable.icon_dot);
        }

        return CIRCLE_ICON;
    }

    private static BitmapDescriptor CIRCLE_ICON;
    private static final Map<EventCategory, BitmapDescriptor> CATEGORY_ICONS = new HashMap<>();
    private static final Map<EventCategory, BitmapDescriptor> CATEGORY_RECOMMENDATION_ICONS =
            new HashMap<>();
}
