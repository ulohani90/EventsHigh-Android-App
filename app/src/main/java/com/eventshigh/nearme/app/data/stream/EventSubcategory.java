package com.eventshigh.nearme.app.data.stream;

import com.eventshigh.nearme.app.data.EventCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 15/03/16.
 */
public enum EventSubcategory {
    ART_SELECT_ALL(EventCategory.ART, "Select All"),
    ART_ART_EXHIBITION(EventCategory.ART, "Art Exhibition"),
    ART_POTTERY(EventCategory.ART, "Pottery"),
    ART_FASHION(EventCategory.ART, "Fashion"),
    ART_PHOTOGRAPHY(EventCategory.ART, "Photography"),
    ART_ART_WORKSHOPS(EventCategory.ART, "Art Workshops"),
    ART_COLLEGE_FESTS(EventCategory.ART, "College Fests"),

    PARTIES_SELECT_ALL(EventCategory.NIGHTLIFE, "Select All"),
    PARTIES_LIVE_MUSIC(EventCategory.NIGHTLIFE, "Live Music"),
    PARTIES_LADIES_NIGHT(EventCategory.NIGHTLIFE, "Ladies Night"),
    PARTIES_KARAOKE(EventCategory.NIGHTLIFE, "Karaoke"),
    PARTIES_BOLLYWOOD(EventCategory.NIGHTLIFE, "Bollywood"),
    PARTIES_DJ(EventCategory.NIGHTLIFE, "DJ"),
    PARTIES_DATING(EventCategory.NIGHTLIFE, "Dating"),

    OUTDOORS_SELECT_ALL(EventCategory.OUTDOORS, "Select All"),
    OUTDOORS_TREKKING(EventCategory.OUTDOORS, "Trekking & Camping"),
    OUTDOORS_CYCLING(EventCategory.OUTDOORS, "Cycling"),
    OUTDOORS_RUNNING(EventCategory.OUTDOORS, "Running"),
    OUTDOORS_ROCK_CLIMBING(EventCategory.OUTDOORS, "Rock Climbing"),
    OUTDOORS_FLEA_MARKET(EventCategory.OUTDOORS, "Flea Markets"),
    OUTDOORS_CITY_WALK(EventCategory.OUTDOORS, "City Walks & Tours"),
    OUTDOORS_ADVENTURE_ACTIVITIES(EventCategory.OUTDOORS, "Adventure Activities"),
    OUTDOORS_STAY_EXPERIENCE(EventCategory.OUTDOORS, "Stay Experience"),

    WORKSHOP_SELECT_ALL(EventCategory.WORKSHOPS, "Select All"),
    WORKSHOP_ART_WORKSHOP(EventCategory.WORKSHOPS, "Art workshops"),
    WORKSHOP_PHOTOGRAPHY(EventCategory.WORKSHOPS, "Photography"),
    WORKSHOP_TECH_WORKSHOP(EventCategory.WORKSHOPS, "Tech Workshops"),
    WORKSHOP_FOOD_WORKSHOP(EventCategory.WORKSHOPS, "Cooking & Food Workshops"),
    WORKSHOP_DANCE_WORKSHOP(EventCategory.WORKSHOPS, "Dance Workshops"),
    WORKSHOP_THEATER_WORKSHOP(EventCategory.WORKSHOPS, "Theatre Workshop"),
    WORKSHOP_SELF_HELP_WORKSHOP(EventCategory.WORKSHOPS, "Self Help Workshop"),
    WORKSHOP_HEALTH_WELLNESS_WORKSHOP(EventCategory.WORKSHOPS, "Health & Wellness Workshop"),
    WORKSHOP_SPORTS_CLASSES(EventCategory.WORKSHOPS, "Sports classes"),



    LIVE_PERFORMANCES_SELECT_ALL(EventCategory.LIVE_PERFORMANCES,"Select All"),
    LIVE_PERFORMANCES_COMEDY_SHOW(EventCategory.LIVE_PERFORMANCES,"Comedy Show"),
    LIVE_PERFORMANCES_MUSIC_CONCERTS(EventCategory.LIVE_PERFORMANCES,"Music Concerts"),
    LIVE_PERFORMANCES_DANCE(EventCategory.LIVE_PERFORMANCES,"Dance"),
    LIVE_PERFORMANCES_THEATRE(EventCategory.LIVE_PERFORMANCES,"Theatre"),
    LIVE_PERFORMANCES_FILMS(EventCategory.LIVE_PERFORMANCES,"Films"),
    LIVE_PERFORMANCES_FASHION_SHOW(EventCategory.LIVE_PERFORMANCES,"Fashion Show"),


    FOOD_SELECT_ALL(EventCategory.FOOD,"Select All"),
    FOOD_FOOD_FESTIVAL(EventCategory.FOOD,"Food Festival"),
    FOOD_BRUNCH(EventCategory.FOOD,"Brunch"),
    FOOD_WINE(EventCategory.FOOD,"Wine"),
    FOOD_FOOD_WALKS(EventCategory.FOOD,"Food walks"),
    FOOD_BEER(EventCategory.FOOD,"Beer"),
    FOOD_BARBEQUE(EventCategory.FOOD,"Barbeque"),


    SPORTS_SELECT_ALL(EventCategory.SPORTS,"Select All"),
    SPORTS_GAMING(EventCategory.SPORTS,"Gaming"),
    SPORTS_FOOTBALL(EventCategory.SPORTS,"Football"),
    SPORTS_CRICKET(EventCategory.SPORTS,"Cricket"),
    SPORTS_CHESS(EventCategory.SPORTS,"Chess"),
    SPORTS_BADMINTON(EventCategory.SPORTS,"Badminton"),
    SPORTS_HOCKEY(EventCategory.SPORTS,"Hockey"),
    SPORTS_SWIMMING(EventCategory.SPORTS,"Swimming"),
    SPORTS_TENNIS(EventCategory.SPORTS,"Tennis"),
    SPORTS_SPORTS_SCREENING(EventCategory.SPORTS,"Sports Screening"),

    HEALTH_SELECT_ALL(EventCategory.HEALTH_WELLNESS,"Select All"),
    HEALTH_YOGA(EventCategory.HEALTH_WELLNESS,"Yoga"),
    HEALTH_FITNESS(EventCategory.HEALTH_WELLNESS,"Fitness"),
    HEALTH_SPRITUAL(EventCategory.HEALTH_WELLNESS,"Spritual"),
    HEALTH_MEDITATION(EventCategory.HEALTH_WELLNESS,"Meditation"),


    LITERATURE_SELECT_ALL(EventCategory.LITERATURE,"Select All"),
    LITERATURE_TECHNOLOGY(EventCategory.LITERATURE,"Technology"),
    LITERATURE_LITERATURE(EventCategory.LITERATURE,"Literature"),
    LITERATURE_TALKS(EventCategory.LITERATURE,"Talks"),
    LITERATURE_POETRY(EventCategory.LITERATURE,"Poetry"),
    LITERATURE_STORY_TELLING(EventCategory.LITERATURE,"Storytelling"),
    LITERATURE_SOCIAL_CAUSES(EventCategory.LITERATURE,"Social Causes"),


    KIDS_SELECT_ALL(EventCategory.KIDS_ENTERTAINMENT,"Select All"),
    KIDS_MAGIC_SHOW(EventCategory.KIDS_ENTERTAINMENT,"Magic Show"),
    KIDS_SUMMER_CAMPS(EventCategory.KIDS_ENTERTAINMENT,"Summer Camps"),
    KIDS_KIDS_WORKSHOP(EventCategory.KIDS_ENTERTAINMENT,"Kids Workshop"),
    KIDS_KIDS_ENTERTAINMENT(EventCategory.KIDS_ENTERTAINMENT,"Kids Entertainment"),
    KIDS_KIDS_SPORTS(EventCategory.KIDS_ENTERTAINMENT,"Kids Sports");


    public final EventCategory category;

    public final String name;

    EventSubcategory(EventCategory category , String name){
        this.category = category;
        this.name = name;
    }

    public static List<EventSubcategory> getEventCategories(EventCategory category,boolean addisAll) {
        List<EventSubcategory> subCategories = new ArrayList<>();


        for (EventSubcategory subcategory : EventSubcategory.values()) {
            if (subcategory.category== category) {
                subCategories.add(subcategory);

            }
        }

        return subCategories;
    }

}
