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
    ADVENTURE,
    ART,
    DANCE,
    FASHION,
    FOOD,
    FOR_COUPLES,
    FOR_FAMILY,
    FOR_KIDS,
    FOR_WOMEN,
    HEALTH,
    LITERATURE,
    MEETUPS,
    MUSIC,
    NIGHTLIFE,
    PHOTOGRAPHY,
    PLAYS,
    SPORTS,
    TECH,
    OTHER;

    /**
     * @return and String ID from resource which represents the category icon from font-awesome.
     */
    public int getIconStringId() {
        switch (this) {
            case ADVENTURE:
                return R.string.fa_road;
            case ART:
                return R.string.fa_photo;
            case DANCE:
                return R.string.fa_paw;
            case FASHION:
                return R.string.fa_eye;
            case FOOD:
                return R.string.fa_cutlery;
            case FOR_COUPLES:
                return R.string.fa_glass;
            case FOR_FAMILY:
                return R.string.fa_home;
            case FOR_KIDS:
                return R.string.fa_child;
            case FOR_WOMEN:
                return R.string.fa_female;
            case HEALTH:
                return R.string.fa_plus_square;
            case LITERATURE:
                return R.string.fa_book;
            case MEETUPS:
                return R.string.fa_users;
            case MUSIC:
                return R.string.fa_music;
            case NIGHTLIFE:
                return R.string.fa_moon_o;
            case PHOTOGRAPHY:
                return R.string.fa_camera;
            case PLAYS:
                return R.string.fa_paw;
            case SPORTS:
                return R.string.fa_soccer_ball_o;
            case TECH:
                return R.string.fa_linux;
        }

        return R.string.fa_calendar;
    }

    /**
     * Get an Icon associated with this Category.
     *
     * @return an BitmapDescriptor for Icon.
     */
    public BitmapDescriptor icon() {
        BitmapDescriptor icon = CATEGORY_ICONS.get(this);
        if (icon == null) {
            int resId = R.drawable.icon_other;
            try {
                resId = R.drawable.class.getField("icon_" + toString().toLowerCase()).getInt(null);
            } catch (IllegalAccessException e) {
                // Ignore
            } catch (NoSuchFieldException e) {
                // Ignore
            }

            icon = BitmapDescriptorFactory.fromResource(resId);
            CATEGORY_ICONS.put(this, icon);
        }
        return icon;
    }

    public static BitmapDescriptor circleIcon() {
        if (CIRCLE_ICON == null) {
            CIRCLE_ICON = BitmapDescriptorFactory.fromResource(R.drawable.dot0);
        }

        return CIRCLE_ICON;
    }

    private static BitmapDescriptor CIRCLE_ICON;
    private static final Map<EventCategory, BitmapDescriptor> CATEGORY_ICONS =
            new HashMap<EventCategory,BitmapDescriptor>();
}
