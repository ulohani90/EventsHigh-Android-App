package com.eventshigh.nearme.app.data;

import android.util.Pair;

import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Collection for events which has tags based filtering in built.
 */
public class EventsCollection {
    public static final String ALL_TAB_NAME = "All";
    public static final EventsCollection EMPTY =
            new EventsCollection(new ArrayList<TaggedEvents>());

    private static class TaggedEvents {
        public final String tabName;
        public final List<Event> events;

        public TaggedEvents(String tabName, List<Event> events) {
            this.tabName = tabName;
            this.events = events;
        }
    }

    public static class Builder {
        private final EventsMarkerManager eventsMarkerManager;
        private final Set<String> whiteListCategories;
        private final Map<String, List<Event>> events = new LinkedHashMap<>();

        public Builder(EventsMarkerManager eventsMarkerManager, Set<String> whiteListCategories) {
            this.eventsMarkerManager = eventsMarkerManager;
            this.whiteListCategories = whiteListCategories;
        }

        public Builder addEvent(Event event) {
            EventMark pref = eventsMarkerManager.getEventMark(event.id);
            if (EventMark.isDismissed(pref)) {
                return this;
            }

            addEvent(ALL_TAB_NAME, event);
            for (String tag : event.tags) {
                if (whiteListCategories.contains(tag.toLowerCase())) {
                    addEvent(tag, event);
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
            List<Event> allEvents = events.remove(ALL_TAB_NAME);
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

            if (allEvents != null) {
                taggedEventsList.add(0, new TaggedEvents(
                        ALL_TAB_NAME, Collections.unmodifiableList(allEvents)));
            }

            return new EventsCollection(taggedEventsList);
        }

        private void addEvent(String tabName, Event event) {
            List<Event> eventList = events.get(tabName);
            if (eventList == null) {
                eventList = new ArrayList<>();
                events.put(tabName, eventList);
            }
            eventList.add(event);
        }
    }


    // A list containing a pair of tag name and list of events associated with that tag.
    private final List<TaggedEvents> taggedEventsList;

    public EventsCollection(List<TaggedEvents> taggedEventsList) {
        this.taggedEventsList = Collections.unmodifiableList(taggedEventsList);
    }

    public boolean isEmpty() {
        return taggedEventsList.isEmpty();
    }

    public List<Pair<String, Integer>> getTabs() {
        List<Pair<String, Integer>> results = new ArrayList<>(taggedEventsList.size());
        for (TaggedEvents categoryEvents : taggedEventsList) {
            results.add(Pair.create(categoryEvents.tabName, categoryEvents.events.size()));
        }
        return results;
    }

    public List<Event> getEvents(int tabPosition) {
        if (tabPosition < taggedEventsList.size()) {
            return taggedEventsList.get(tabPosition).events;
        } else {
            return new ArrayList<>();
        }
    }
}
