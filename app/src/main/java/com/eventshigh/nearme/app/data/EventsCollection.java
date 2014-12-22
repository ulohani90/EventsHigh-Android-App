package com.eventshigh.nearme.app.data;

import android.util.Pair;

import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Collection for events which has tags based filtering in built.
 */
public class EventsCollection {
    public static final String ALL_EVENTS_CATEGORY = "All";
    public static final String RECOMMENDED_EVENTS_CATEGORY = "Recommended";

    private static final Set<String> TAGS_BLACKLIST = new HashSet<>();
    static {
        TAGS_BLACKLIST.add("courses");
    }

    public static class Builder {
        private final boolean showAllTags;
        private final Map<String, List<Event>> events = new HashMap<>();

        public Builder(boolean showAllTags) {
            this.showAllTags = showAllTags;
        }

        public Builder addEvent(Event event) {

            addEvent(ALL_EVENTS_CATEGORY, event);
            if (event.ehRecommended) {
                addEvent(RECOMMENDED_EVENTS_CATEGORY, event);
            }

            if (showAllTags) {
                for (String tag : event.getAllTags()) {
                    if (! TAGS_BLACKLIST.contains(tag.toLowerCase())) {
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

            if (allEvents != null) {
                tagEventsPairs.add(0, Pair.create(
                        ALL_EVENTS_CATEGORY, Collections.unmodifiableList(allEvents)));
            }
            if (recommendedEvents != null) {
                tagEventsPairs.add(1, Pair.create(
                        RECOMMENDED_EVENTS_CATEGORY, Collections.unmodifiableList(recommendedEvents)));
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
