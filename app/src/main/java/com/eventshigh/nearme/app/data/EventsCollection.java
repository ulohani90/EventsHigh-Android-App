package com.eventshigh.nearme.app.data;

import android.util.Pair;

import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

/**
 * Collection for events which has tags based filtering in built.
 */
public class EventsCollection {
    public static final String ALL_EVENTS_CATEGORY = "All";
    public static final String RECOMMENDED_EVENTS_CATEGORY = "Recommended";
    public static final String NOW_EVENTS_CATEGORY = "Starting Soon";
    public static final String TODAY_EVENTS_CATEGORY = "Today";

    private static final Set<String> TAGS_BLACKLIST = new HashSet<>();
    static {
        TAGS_BLACKLIST.add("courses");
    }

    // In NOW_EVENTS_CATEGORY, we show events which are starting in
    // SOON_THRESHOLD_SEC seconds.
    private static final int SOON_THRESHOLD_SEC = 4 * 3600;

    public static class Builder {
        private final Set<String> whiteListedTagCategories;
        private final long nowTimestamp;
        private final long latestByTimestamp;
        private final boolean showStartingSoon;
        private final Map<String, List<Event>> events = new HashMap<>();

        public Builder(City city, Set<String> whiteListedTagCategories) {
            this.whiteListedTagCategories = whiteListedTagCategories;
            this.nowTimestamp = new Date().getTime();
            latestByTimestamp = nowTimestamp + SOON_THRESHOLD_SEC * 1000L;

            // Show the starting Soon Section after 4pm.
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getTimeZone(city.timeZone));
            showStartingSoon = cal.get(Calendar.HOUR_OF_DAY) >= 16;
        }

        public Builder addEvent(Event event) {
            addEvent(ALL_EVENTS_CATEGORY, event);
            if (event.ehRecommended) {
                addEvent(RECOMMENDED_EVENTS_CATEGORY, event);
            }

            if (showStartingSoon) {
                // See if this events is starting soon.
                for (long eventTime : event.eventTimings) {
                    if (eventTime < nowTimestamp) {
                        continue;
                    }

                    if (eventTime <= latestByTimestamp &&
                            DateTimeUtils.dateToEventTime(new Date(eventTime),
                                    TimeZone.getTimeZone(event.city.timeZone)).time != null) {
                        addEvent(NOW_EVENTS_CATEGORY, event);
                    }
                    break;
                }
            } else {
                // See if this events is happening today.
                if (nowTimestamp > DateTimeUtils.getEventDate(event, 0).getTime()) {
                    addEvent(TODAY_EVENTS_CATEGORY, event);
                }
            }

            if (!whiteListedTagCategories.isEmpty()) {
                for (String tag : event.getAllTags()) {
                    if (whiteListedTagCategories.contains(tag.toLowerCase()) &&
                        !TAGS_BLACKLIST.contains(tag.toLowerCase())) {
                        addEvent(Utils.capitalize(tag), event);
                    }
                }
            }

            return this;
        }

        public Builder addAllEvent(Collection<Event> eventsCollection) {
            for (Event event : eventsCollection) {
                addEvent(event);
            }
            return this;
        }

        public EventsCollection build() {
            List<Pair<String, List<Event>>> tagEventsPairs = new ArrayList<>(events.keySet().size());

            List<Event> allEvents = events.remove(ALL_EVENTS_CATEGORY);
            List<Event> recommendedEvents = events.remove(RECOMMENDED_EVENTS_CATEGORY);
            List<Event> nowEvents = events.remove(NOW_EVENTS_CATEGORY);
            List<Event> todayEvents = events.remove(TODAY_EVENTS_CATEGORY);
            for (Entry<String, List<Event>> tagEvents : events.entrySet()) {
                if (tagEvents.getValue().size() > 1) {
                    tagEventsPairs.add(Pair.create(
                            tagEvents.getKey(), Collections.unmodifiableList(tagEvents.getValue())));
                }
            }

            Collections.sort(tagEventsPairs, new Comparator<Pair<String, List<Event>>>() {
                @Override
                public int compare(Pair<String, List<Event>> lhs, Pair<String, List<Event>> rhs) {
                    return Integer.valueOf(rhs.second.size()).compareTo(lhs.second.size());
                }
            });

            int index = 0;
            if (allEvents != null) {
                tagEventsPairs.add(index, Pair.create(
                        ALL_EVENTS_CATEGORY, Collections.unmodifiableList(allEvents)));
                index ++;
            }
            if (recommendedEvents != null) {
                tagEventsPairs.add(index, Pair.create(
                        RECOMMENDED_EVENTS_CATEGORY, Collections.unmodifiableList(recommendedEvents)));
                index ++;
            }
            if (nowEvents != null) {
                tagEventsPairs.add(index, Pair.create(
                        NOW_EVENTS_CATEGORY, Collections.unmodifiableList(nowEvents)));
                index ++;
            }
            if (todayEvents != null && allEvents != null && allEvents.size() > todayEvents.size()) {
                tagEventsPairs.add(index, Pair.create(
                        TODAY_EVENTS_CATEGORY, Collections.unmodifiableList(todayEvents)));
            }

            return new EventsCollection(tagEventsPairs);
        }

        private void addEvent(String tag, Event event) {
            List<Event> eventList = events.get(tag);
            if (eventList == null) {
                eventList = new ArrayList<>();
                events.put(tag, eventList);
            }
            eventList.add(event);
        }
    }

    // A list containing a pair of tag name and list of events associated with that tag..
    private final List<Pair<String, List<Event>>> tagEventsPairs;

    private EventsCollection(List<Pair<String, List<Event>>> tagEventsPairs) {
        this.tagEventsPairs = tagEventsPairs;
    }

    public List<Pair<String, Integer>> getTags() {
        List<Pair<String, Integer>> results = new ArrayList<>(tagEventsPairs.size());
        for (Pair<String, List<Event>> categoryEvents : tagEventsPairs) {
            results.add(Pair.create(categoryEvents.first, categoryEvents.second.size()));
        }
        return results;
    }

    public List<Event> getEvents(int tagPosition) {
        if (tagPosition < tagEventsPairs.size()) {
            return tagEventsPairs.get(tagPosition).second;
        } else {
            return new ArrayList<>();
        }
    }
}
