package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.eventshigh.nearme.app.user.UserActionHelper;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the user personalization -- his favourite and dismissed events. The favourite or
 * dismissed information is stored as {@link EventMark}. {@link EventMark} can be updated
 * through {@link Editor} class.
 *
 * It is very efficient to check for event mark as it is kept in-memory and backed by
 * DB for persistent storage.
 */
public class EventsMarkerManager {

    /**
     * Event can be marked as favourite or dismissed by user. This enum lists marks supported.
     * To simplify the DB storage, each mark has int value which is used while persisting
     * to DB.
     */
    public enum EventMark {
        FAVOURITE (1),
        DISMISSED (2);

        public final int value;

        EventMark(int value) {
            this.value = value;
        }

        public static boolean isFavourite(@Nullable EventMark eventMark) {
            return eventMark != null && eventMark == FAVOURITE;
        }

        public static @Nullable EventMark getPrefFromValue(int value) {
            for (EventMark pref : EventMark.values()) {
                if (pref.value == value) {
                    return pref;
                }
            }

            return null;
        }
    }

    /**
     * Supports the modification for event marks. Each instance of editor should be
     * closed after its not needed.
     */
    public class Editor implements Closeable {
        private final SQLiteDatabase database;
        private final List<Thread> threads = new ArrayList<>();
        private final Context context;

        private Editor(Context context) {
            database = new EventMarkDbHelper(context).getWritableDatabase();
            this.context = context;
        }

        public void close() {
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    // do nothing.
                }
            }
            database.close();
        }

        public Editor recordEventMark(Event event, @Nullable EventMark mark) {
            if (mark == null) {
                removeEventMark(event);
            } else {
                eventMarkMap.put(event.id, mark);
                if (EventMark.isFavourite(mark)) {
                    new UserActionHelper(context).recordAction(
                            UserActionHelper.EventAction.ADD_FAVORITE, event.id);
                }
                threads.add(EventMarkDbHelper.addEntry(database, event.id, mark));
            }
            return this;
        }

        public Editor removeEventMark(Event event) {
            EventMark mark = eventMarkMap.remove(event.id);
            if (EventMark.isFavourite(mark)) {
                new UserActionHelper(context).recordAction(
                        UserActionHelper.EventAction.REMOVE_FAVORITE, event.id);
            }
            threads.add(EventMarkDbHelper.removeEntry(database, event.id));
            return this;
        }
    }

    /**
     * Singleton Instance.
     */
    private static EventsMarkerManager instance;
    public static synchronized EventsMarkerManager getInstance(Context context) {
        if (instance == null) {
            instance = new EventsMarkerManager(context);
        }

        return instance;
    }

    private final Context context;
    private final Map<String, EventMark> eventMarkMap = new ConcurrentHashMap<>();
    private boolean loaded = false;

    private EventsMarkerManager(Context context) {
        this.context = context.getApplicationContext();

        // Start new thread to load events.
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    refreshListingFromDb();
                    loaded = true;
                    this.notifyAll();
                }
            }
        }).start();
    }

    // Waits for data to be loaded from DB.
    public synchronized void waitForLoading() {
        while (!loaded) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                // ignore.
            }
        }
    }

    public @Nullable EventMark getEventMark(String eventId) {
        return eventMarkMap.get(eventId);
    }

    public Editor getEditor() {
        return new Editor(context);
    }

    public boolean isFavourite(String eventId) {
        return EventMark.isFavourite(getEventMark(eventId));
    }

    public List<String> getFavouritedEvents() {
        List<String> favouritedEvents = new ArrayList<>();
        for (Entry<String, EventMark> entry : eventMarkMap.entrySet()) {
            if (EventMark.isFavourite(entry.getValue())) {
                favouritedEvents.add(entry.getKey());
            }
        }

        return favouritedEvents;
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private void refreshListingFromDb() {
        SQLiteDatabase database = new EventMarkDbHelper(this.context).getReadableDatabase();
        try {
            eventMarkMap.clear();
            for (Pair<String, EventMark> entry : EventMarkDbHelper.fetchAllEntries(database)) {
                if (entry.second != null) {
                    eventMarkMap.put(entry.first, entry.second);
                }
            }
        } finally {
            database.close();
        }
    }
}
