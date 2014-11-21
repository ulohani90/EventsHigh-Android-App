package com.eventshigh.nearme.app.data;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View.MeasureSpec;
import android.widget.TextView;

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
    SPIRITUAL,
    SPORTS,
    TECH,
    WORKSHOPS,
    OTHER;

    public static EventCategory fromString(String categoryString) {
        EventCategory category = ALL_CATEGORIES.get(categoryString.toLowerCase());
        if (category == null) {
            return EventCategory.OTHER;
        }

        return  category;
    }

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
            case SPIRITUAL:
                return R.string.fa_empire;
            case SPORTS:
                return R.string.fa_soccer_ball_o;
            case TECH:
                return R.string.fa_linux;
            case WORKSHOPS:
                return R.string.fa_institution;
        }

        return R.string.fa_calendar;
    }

    /**
     * Get an Icon associated with this Category.
     * @param inflater the inflater to be used to generate Icon.
     * @param font font-awesome font from assets.
     * @param small true if you need smaller version of icon
     * @return an BitmapDescriptor for Icon.
     */
    public BitmapDescriptor icon(LayoutInflater inflater, Typeface font, boolean small) {
        Pair<BitmapDescriptor, BitmapDescriptor> icons = CATEGORY_ICONS.get(this);
        if (icons == null) {
            icons = Pair.create(
                    getCategoryIcon(inflater, font, true),
                    getCategoryIcon(inflater, font, false));
            CATEGORY_ICONS.put(this, icons);
        }
        return small ? icons.first : icons.second;
    }

    private static Map<String, EventCategory> ALL_CATEGORIES = new HashMap<String, EventCategory>();
    static {
        for (EventCategory category : EventCategory.values()) {
            ALL_CATEGORIES.put(category.toString().replace('_', ' ').toLowerCase(), category);
        }
    }

    private static Map<EventCategory, Pair<BitmapDescriptor, BitmapDescriptor>> CATEGORY_ICONS =
            new HashMap<EventCategory, Pair<BitmapDescriptor, BitmapDescriptor>>();
    private BitmapDescriptor getCategoryIcon(LayoutInflater inflater, Typeface font, boolean small) {
        TextView view = (TextView) inflater.inflate(R.layout.category_icon, null);
        view.setTypeface(font);
        view.setText(getIconStringId());
        if (small) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.getTextSize() / 2);
        }
        int specWidth = MeasureSpec.makeMeasureSpec(0 /* any */, MeasureSpec.UNSPECIFIED);
        view.measure(specWidth, specWidth);
        Bitmap bitmap = Bitmap.createBitmap(
                view.getMeasuredWidth(),
                view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
