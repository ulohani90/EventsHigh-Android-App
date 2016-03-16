package com.eventshigh.nearme.app.data.stream;

import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 15/03/16.
 */
public enum EventSubcategory {
    ART_CATEGORY1(EventCategory.ART, "Category 1"),
    ART_CATEGORY2(EventCategory.ART, "Category 2"),
    PARTIES_CATEGORY1(EventCategory.PARTIES, "Category 1"),
    PARTIES_CATEGORY2(EventCategory.PARTIES, "Category 2"),
    THEATER_CATEGORY1(EventCategory.THEATRE, "Category 1"),
    THEATER_CATEGORY2(EventCategory.THEATRE, "Category 2"),
    MUSIC_CATEGORY1(EventCategory.MUSIC, "Category 1"),
    MUSIC_CATEGORY2(EventCategory.MUSIC, "Category 2"),
    SPORTS_CATEGORY1(EventCategory.SPORTS, "Category 1"),
    SPORTS_CATEGORY2(EventCategory.SPORTS, "Category 2"),;


    public final EventCategory category;

    public final String name;

    EventSubcategory(EventCategory category , String name){
        this.category = category;
        this.name = name;
    }

    public static List<EventSubcategory> getEventCategories(EventCategory category) {
        List<EventSubcategory> subCategories = new ArrayList<>();
        for (EventSubcategory subcategory : EventSubcategory.values()) {
            if (subcategory.category== category) {
                subCategories.add(subcategory);

            }
        }

        return subCategories;
    }

}
