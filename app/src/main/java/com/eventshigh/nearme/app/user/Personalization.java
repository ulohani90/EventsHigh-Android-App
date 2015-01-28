package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
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
        DISMISSED
    }

    public interface OnEventStateListener {
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
    private final List<OnEventStateListener> eventStateListeners = new ArrayList<>();

    private Personalization(Context context) {
        this.context = context.getApplicationContext();
    }

    public void recordPref(String eventId, @Nullable UserEventPref pref) {
        if (pref == null) {
            removePref(eventId);
        } else {
            eventPrefMap.put(eventId, pref);
        }
        callListeners(eventId);
    }

    public void removePref(String eventId) {
        eventPrefMap.remove(eventId);
        callListeners(eventId);
    }

    public @Nullable UserEventPref getPref(String eventId) {
        return eventPrefMap.get(eventId);
    }

    public synchronized void addOnEventStateChangeListener(OnEventStateListener listener) {
        eventStateListeners.add(listener);
    }

    public synchronized boolean removeOnEventStateChangeListener(OnEventStateListener listener) {
        return eventStateListeners.remove(listener);
    }

    public boolean isFavourite(String eventId) {
        return isFavourite(getPref(eventId));
    }

    public boolean isDismissed(String eventId) {
        return isFavourite(getPref(eventId));
    }

    public static boolean isFavourite(@Nullable UserEventPref pref) {
        return pref != null && pref == UserEventPref.LIKED;
    }

    public static boolean isDismissed(@Nullable UserEventPref pref) {
        return pref != null && pref == UserEventPref.DISMISSED;
    }

    private synchronized void callListeners(String eventId) {
        UserEventPref pref = getPref(eventId);
        for (OnEventStateListener listener : eventStateListeners) {
            listener.onEventStateChange(eventId, pref);
        }
    }
}
