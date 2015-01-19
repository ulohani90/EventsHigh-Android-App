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

    public static class TaggedEvents {
        public final String tag;
        public final List<Event> events;

        public TaggedEvents(String tag, List<Event> events) {
            this.tag = tag;
            this.events = events;
        }
    }

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
            List<TaggedEvents> taggedEventsList = new ArrayList<>(events.keySet().size());

            List<Event> allEvents = events.remove(ALL_EVENTS_CATEGORY);
            List<Event> recommendedEvents = events.remove(RECOMMENDED_EVENTS_CATEGORY);
            List<Event> nowEvents = events.remove(NOW_EVENTS_CATEGORY);
            List<Event> todayEvents = events.remove(TODAY_EVENTS_CATEGORY);
            for (Entry<String, List<Event>> tagEvents : events.entrySet()) {
                if (tagEvents.getValue().size() > 1) {
                    taggedEventsList.add(new TaggedEvents(
                            tagEvents.getKey(), Collections.unmodifiableList(tagEvents.getValue())));
                }
            }

            Collections.sort(taggedEventsList, new Comparator<TaggedEvents>() {
                @Override
                public int compare(TaggedEvents lhs, TaggedEvents rhs) {
                    return Integer.valueOf(rhs.events.size()).compareTo(lhs.events.size());
                }
            });

            int index = 0;
            if (allEvents != null) {
                taggedEventsList.add(index, new TaggedEvents(
                        ALL_EVENTS_CATEGORY, Collections.unmodifiableList(allEvents)));
                index ++;
            }
            if (recommendedEvents != null) {
                taggedEventsList.add(index, new TaggedEvents(
                        RECOMMENDED_EVENTS_CATEGORY, Collections.unmodifiableList(recommendedEvents)));
                index ++;
            }
            if (nowEvents != null) {
                taggedEventsList.add(index, new TaggedEvents(
                        NOW_EVENTS_CATEGORY, Collections.unmodifiableList(nowEvents)));
                index ++;
            }
            if (todayEvents != null && allEvents != null && allEvents.size() > todayEvents.size()) {
                taggedEventsList.add(index, new TaggedEvents(
                        TODAY_EVENTS_CATEGORY, Collections.unmodifiableList(todayEvents)));
            }

            return new EventsCollection(taggedEventsList);
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

    // A list containing a pair of tag name and list of events associated with that tag.
    private final List<TaggedEvents> taggedEventsList;

    public EventsCollection(List<TaggedEvents> taggedEventsList) {
        this.taggedEventsList = taggedEventsList;
    }

    public List<Pair<String, Integer>> getTags() {
        List<Pair<String, Integer>> results = new ArrayList<>(taggedEventsList.size());
        for (TaggedEvents categoryEvents : taggedEventsList) {
            results.add(Pair.create(categoryEvents.tag, categoryEvents.events.size()));
        }
        return results;
    }

    public List<Event> getEvents(int tagPosition) {
        if (tagPosition < taggedEventsList.size()) {
            return taggedEventsList.get(tagPosition).events;
        } else {
            return new ArrayList<>();
        }
    }
}
