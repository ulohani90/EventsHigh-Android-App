package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
            new EventsCollection(new ArrayList<TaggedEvents>(), new ArrayList<TagInfo>());

    public static class TaggedEvents {
        public final EventTab tab;
        public final List<Event> events;

        public TaggedEvents(EventTab tab, List<Event> events) {
            this.tab = tab;
            this.events = events;
        }
    }

    public static class TagInfo implements Parcelable  {
        public final String tagName;
        public final int numEvents;

        public TagInfo(String tagName, int numEvents) {
            this.tagName = tagName;
            this.numEvents = numEvents;
        }

        public String toString() {
            return tagName + " (" + numEvents + ")";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(tagName);
            dest.writeInt(numEvents);
        }

        public static final Parcelable.Creator<TagInfo> CREATOR =
            new Parcelable.Creator<TagInfo>() {
                public TagInfo createFromParcel(Parcel in) {
                    return new TagInfo(in.readString(), in.readInt());
                }

                public TagInfo[] newArray(int size) {
                    return new TagInfo[size];
                }
            };
    }

    private static class Counter {
        private int count = 0;

        public int increment() {
            count ++;
            return count;
        }

        public int toInt() {
            return count;
        }
    }

    public static class Builder {
        private final EventsMarkerManager eventsMarkerManager;
        private final boolean showToday;
        private final Set<String> whiteListedTagCategories;
        private final long tomorrowMidnightTimestamp;
        private final Map<EventTab, List<Event>> events = new LinkedHashMap<>();
        private final Map<String, Counter> categories = new HashMap<>();

        public Builder(City city, EventsMarkerManager eventsMarkerManager, boolean showToday, Set<String> whiteListedTagCategories) {
            this.eventsMarkerManager = eventsMarkerManager;
            this.showToday = showToday;
            this.whiteListedTagCategories = whiteListedTagCategories;

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

            // Temporary: Do not show RECOMMENDED tab.
            /**
            if (event.ehRecommended) {
                addEvent(EventTab.RECOMMENDED, event);
            }
            **/

            // Temporary: Do not show Today tab.
            /**
            // See if this events is happening today.
            if (showToday &&
                event.eventTimings.length > 0 &&
                event.eventTimings[0] < tomorrowMidnightTimestamp) {
                addEvent(EventTab.TODAY, event);
            }
            **/

            if (! whiteListedTagCategories.isEmpty()) {
                for (String tag : event.tags) {
                    if (whiteListedTagCategories.contains(tag.toLowerCase())) {
                        incrementCounter(Utils.capitalize(tag));
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

            List<TagInfo> tagInfos = new ArrayList<>(categories.size());
            for (Entry<String, Counter> category : categories.entrySet()) {
                if (category.getValue().toInt() > 1) {
                    tagInfos.add(new TagInfo(category.getKey(), category.getValue().toInt()));
                }
            }
            Collections.sort(tagInfos, new Comparator<TagInfo>() {
                @Override
                public int compare(TagInfo lhs, TagInfo rhs) {
                    return Integer.valueOf(rhs.numEvents).compareTo(lhs.numEvents);
                }
            });


            return new EventsCollection(taggedEventsList, tagInfos);
        }

        private void addEvent(EventTab tab, Event event) {
            List<Event> eventList = events.get(tab);
            if (eventList == null) {
                eventList = new ArrayList<>();
                events.put(tab, eventList);
            }
            eventList.add(event);
        }

        private void incrementCounter(String tag) {
            Counter counter = categories.get(tag);
            if (counter == null) {
                counter = new Counter();
                categories.put(tag, counter);
            }
            counter.increment();
        }
    }


    // A list containing a pair of tag name and list of events associated with that tag.
    private final List<TaggedEvents> taggedEventsList;
    private final List<TagInfo> tagInfos;

    public EventsCollection(List<TaggedEvents> taggedEventsList, List<TagInfo> tagInfos) {
        this.taggedEventsList = Collections.unmodifiableList(taggedEventsList);
        this.tagInfos = Collections.unmodifiableList(tagInfos);
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

    public List<TagInfo> getTagInfos() {
        return tagInfos;
    }
}
