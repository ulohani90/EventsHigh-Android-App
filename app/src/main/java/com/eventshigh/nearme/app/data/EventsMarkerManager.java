package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
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
 *
 * This class also supports for listening to marker changes via {@link OnEventMarkChangeListener}.
 */
public class EventsMarkerManager {

    /**
     * Event can be marked as favourite or dismissed by user. This enum lists marks supported.
     * To simplify the DB storage, each mark has int value which is used while persisting
     * to DB.
     */
    public static enum EventMark {
        FAVOURITE (1),
        DISMISSED (2);

        public final int value;

        private EventMark(int value) {
            this.value = value;
        }

        public static boolean isDismissed(@Nullable EventMark eventMark) {
            return eventMark != null && eventMark == DISMISSED;
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
     * A listener which can be registered to listen for changes on EventMark.
     */
    public interface OnEventMarkChangeListener {
        void onEventStateChange(String eventId, @Nullable EventMark eventMark);
    }

    /**
     * Supports the modification for event marks. Each instance of editor should be
     * closed after its not needed.
     */
    public class Editor implements Closeable {
        private final SQLiteDatabase database;
        private final List<Thread> threads = new ArrayList<>();

        private Editor(Context context) {
            database = new EventMarkDbHelper(context).getWritableDatabase();
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

        public Editor recordEventMark(String eventId, @Nullable EventMark mark) {
            if (mark == null) {
                removeEventMark(eventId);
            } else {
                eventMarkMap.put(eventId, mark);
                callListeners(eventId, mark);
                threads.add(EventMarkDbHelper.addEntry(database, eventId, mark));
            }
            return this;
        }

        public Editor removeEventMark(String eventId) {
            eventMarkMap.remove(eventId);
            callListeners(eventId, null);
            threads.add(EventMarkDbHelper.removeEntry(database, eventId));
            return this;
        }

        public EventsMarkerManager getEventsMarkerManager() {
            return EventsMarkerManager.this;
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
    private boolean loaded = false;
    private final Map<String, EventMark> eventMarkMap = new ConcurrentHashMap<>();
    private final List<OnEventMarkChangeListener> eventMarkChangeListeners = new ArrayList<>();

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

    /**
     * Waits for data to be loaded from DB.
     */
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

    public void addOnEventMarkChangeListener(OnEventMarkChangeListener listener) {
        synchronized (eventMarkChangeListeners) {
            eventMarkChangeListeners.add(listener);
        }
    }

    public boolean removeOnEventMarkChangeListener(OnEventMarkChangeListener listener) {
        synchronized (eventMarkChangeListeners) {
            return eventMarkChangeListeners.remove(listener);
        }
    }

    public boolean isFavourite(String eventId) {
        return EventMark.isFavourite(getEventMark(eventId));
    }

    public boolean isDismissed(String eventId) {
        return EventMark.isDismissed(getEventMark(eventId));
    }

    public void removeDismissed(List<Event> events) {
        for (Iterator<Event> it =  events.iterator(); it.hasNext(); ) {
            Event event = it.next();
            if (isDismissed(event.id)) {
                it.remove();
            }
        }
    }

    public void restoreAll() {
        EventMarkDbHelper.restoreAll(context);

        Iterator<Entry<String, EventMark>> it = eventMarkMap.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() == EventMark.DISMISSED) {
                it.remove();
            }
        }
    }

    private void callListeners(String eventId, @Nullable EventMark pref) {
        synchronized (eventMarkChangeListeners) {
            for (OnEventMarkChangeListener listener : eventMarkChangeListeners) {
                listener.onEventStateChange(eventId, pref);
            }
        }
    }

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
