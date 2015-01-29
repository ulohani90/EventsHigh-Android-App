package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.data.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Records the user personalization -- his likes, dislikes etc.
 *
 * TODO: Use persistent store.
 */
public class Personalization {
    public enum UserEventPref {
        LIKED,
        DISMISSED;

        public static boolean isDismissed(@Nullable UserEventPref pref) {
            return pref != null && pref == DISMISSED;
        }

        public static boolean isFavourite(@Nullable UserEventPref pref) {
            return pref != null && pref == LIKED;
        }
    }

    public interface OnEventStateChangeListener {
        void onEventStateChange(String eventId, @Nullable UserEventPref pref);
    }

    /**
     * Singleton Instance.
     */
    private static Personalization instance;
    public static synchronized Personalization getInstance(Context context) {
        if (instance == null) {
            instance = new Personalization(context);
        }

        return instance;
    }

    private final Context context;
    private final Map<String, UserEventPref> eventPrefMap = new HashMap<>();
    private final List<OnEventStateChangeListener> eventStateListeners = new ArrayList<>();

    private Personalization(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized void recordPref(String eventId, @Nullable UserEventPref pref) {
        if (pref == null) {
            removePref(eventId);
        } else {
            eventPrefMap.put(eventId, pref);
        }
        callListeners(eventId);
    }

    public synchronized void removePref(String eventId) {
        eventPrefMap.remove(eventId);
        callListeners(eventId);
    }

    public synchronized @Nullable UserEventPref getPref(String eventId) {
        return eventPrefMap.get(eventId);
    }

    public synchronized void addOnEventStateChangeListener(OnEventStateChangeListener listener) {
        eventStateListeners.add(listener);
    }

    public synchronized boolean removeOnEventStateChangeListener(OnEventStateChangeListener listener) {
        return eventStateListeners.remove(listener);
    }

    public boolean isFavourite(String eventId) {
        return UserEventPref.isFavourite(getPref(eventId));
    }

    public boolean isDismissed(String eventId) {
        return UserEventPref.isDismissed(getPref(eventId));
    }

    public void filterDismissed(Collection<? extends Event> events) {
        for(Iterator<? extends Event> it = events.iterator(); it.hasNext();) {
            Event event = it.next();
            if (isDismissed(event.id)) {
                it.remove();
            }
        }
    }

    private synchronized void callListeners(String eventId) {
        UserEventPref pref = getPref(eventId);
        for (OnEventStateChangeListener listener : eventStateListeners) {
            listener.onEventStateChange(eventId, pref);
        }
    }
}
