package com.eventshigh.nearme.app.data;

import android.util.Pair;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Collection for events which has tags based filtering in built.
 */
public class EventsCollection {
    public static enum EventTab {
        ALL(android.R.color.holo_green_dark),
        RECOMMENDED(android.R.color.holo_orange_dark),
        FAVOURITES(android.R.color.holo_red_dark),
        TODAY(R.color.ripple_dark);

        public final int colorId;

        EventTab(int colorId) {
            this.colorId = colorId;
        }
    }

    public static final EventsCollection EMPTY =
            new EventsCollection(new ArrayList<TaggedEvents>());

    public static class TaggedEvents {
        public final EventTab tab;
        public final List<Event> events;

        public TaggedEvents(EventTab tab, List<Event> events) {
            this.tab = tab;
            this.events = events;
        }
    }

    public static class Builder {
        private final EventsMarkerManager eventsMarkerManager;
        private final boolean showToday;
        private final long tomorrowMidnightTimestamp;
        private final Map<EventTab, List<Event>> events = new LinkedHashMap<>();

        public Builder(City city, EventsMarkerManager eventsMarkerManager, boolean showToday) {
            this.eventsMarkerManager = eventsMarkerManager;
            this.showToday = showToday;

            Calendar midnight = DateTimeUtils.toMidnight(Calendar.getInstance(), city.timeZone);
            midnight.add(Calendar.DAY_OF_MONTH, 1);
            tomorrowMidnightTimestamp = midnight.getTime().getTime();

            events.put(EventTab.ALL, new ArrayList<Event>());
            events.put(EventTab.RECOMMENDED, new ArrayList<Event>());
            events.put(EventTab.TODAY, new ArrayList<Event>());
        }

        public Builder addEvent(Event event) {
            EventMark pref = eventsMarkerManager.getEventMark(event.id);
            if (EventMark.isDismissed(pref)) {
                return this;
            }

            addEvent(EventTab.ALL, event);

            // TODO: check we should show RECOMMENDED tab.
            /**
            if (event.ehRecommended) {
                addEvent(EventTab.RECOMMENDED, event);
            }
            **/

            // See if this events is happening today.
            if (showToday &&
                event.eventTimings.length > 0 &&
                event.eventTimings[0] < tomorrowMidnightTimestamp) {
                addEvent(EventTab.TODAY, event);
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
            for (Entry<EventTab, List<Event>> tagEvents : events.entrySet()) {
                if (!tagEvents.getValue().isEmpty()) {
                    taggedEventsList.add(new TaggedEvents(
                            tagEvents.getKey(), Collections.unmodifiableList(tagEvents.getValue())));
                }
            }
            if (taggedEventsList.size() > 0) {
                boolean isRecommendedShown = taggedEventsList.size() > 1 &&
                        taggedEventsList.get(1).tab == EventTab.RECOMMENDED;
                taggedEventsList.add(isRecommendedShown ? 2 : 1, new TaggedEvents(
                        EventTab.FAVOURITES, taggedEventsList.get(0).events));
            }

            return new EventsCollection(taggedEventsList);
        }

        private void addEvent(EventTab tab, Event event) {
            List<Event> eventList = events.get(tab);
            if (eventList == null) {
                eventList = new ArrayList<>();
                events.put(tab, eventList);
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

    public List<Pair<EventTab, Integer>> getTabs() {
        List<Pair<EventTab, Integer>> results = new ArrayList<>(taggedEventsList.size());
        for (TaggedEvents categoryEvents : taggedEventsList) {
            results.add(Pair.create(categoryEvents.tab, categoryEvents.events.size()));
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
