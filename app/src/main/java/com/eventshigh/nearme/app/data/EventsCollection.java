package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

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
    public static final String ALL_EVENTS_CATEGORY = "All";
    public static final String RECOMMENDED_EVENTS_CATEGORY = "Recommended";
    public static final String FAVOURITE_EVENTS_CATEGORY = "Favourite";
    public static final String TODAY_EVENTS_CATEGORY = "Today";
    public static final EventsCollection EMPTY =
            new EventsCollection(new ArrayList<TaggedEvents>(), new ArrayList<TagInfo>());

    public static class TaggedEvents {
        public final String tag;
        public final List<Event> events;

        public TaggedEvents(String tag, List<Event> events) {
            this.tag = tag;
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
        private final Map<String, List<Event>> events = new LinkedHashMap<>();
        private final Map<String, Counter> categories = new HashMap<>();

        public Builder(City city, EventsMarkerManager eventsMarkerManager, boolean showToday, Set<String> whiteListedTagCategories) {
            this.eventsMarkerManager = eventsMarkerManager;
            this.showToday = showToday;
            this.whiteListedTagCategories = whiteListedTagCategories;

            Calendar midnight = DateTimeUtils.toMidnight(Calendar.getInstance(), city.timeZone);
            midnight.add(Calendar.DAY_OF_MONTH, 1);
            tomorrowMidnightTimestamp = midnight.getTime().getTime();

            events.put(ALL_EVENTS_CATEGORY, new ArrayList<Event>());
            events.put(RECOMMENDED_EVENTS_CATEGORY, new ArrayList<Event>());
            events.put(TODAY_EVENTS_CATEGORY, new ArrayList<Event>());
        }

        public Builder addEvent(Event event) {
            EventMark pref = eventsMarkerManager.getEventMark(event.id);
            if (EventMark.isDismissed(pref)) {
                return this;
            }

            addEvent(ALL_EVENTS_CATEGORY, event);
            if (event.ehRecommended) {
                addEvent(RECOMMENDED_EVENTS_CATEGORY, event);
            }

            // See if this events is happening today.
            if (showToday &&
                event.eventTimings.length > 0 &&
                event.eventTimings[0] < tomorrowMidnightTimestamp) {
                addEvent(TODAY_EVENTS_CATEGORY, event);
            }

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
            for (Entry<String, List<Event>> tagEvents : events.entrySet()) {
                if (!tagEvents.getValue().isEmpty()) {
                    taggedEventsList.add(new TaggedEvents(
                            tagEvents.getKey(), Collections.unmodifiableList(tagEvents.getValue())));
                }
            }
            if (taggedEventsList.size() > 0) {
                boolean isRecommededShown = taggedEventsList.size() > 1 &&
                        taggedEventsList.get(1).tag.equals(RECOMMENDED_EVENTS_CATEGORY);
                taggedEventsList.add(isRecommededShown ? 2 : 1, new TaggedEvents(
                        FAVOURITE_EVENTS_CATEGORY, taggedEventsList.get(0).events));
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

        private void addEvent(String tag, Event event) {
            List<Event> eventList = events.get(tag);
            if (eventList == null) {
                eventList = new ArrayList<>();
                events.put(tag, eventList);
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

    public List<TagInfo> getTagInfos() {
        return tagInfos;
    }
}
