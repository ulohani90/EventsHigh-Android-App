package com.eventshigh.nearme.app.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.network.EventSuggestRequest;
import com.eventshigh.nearme.app.network.EventSuggestRequest.SuggestEvent;
import com.eventshigh.nearme.app.network.TagsSuggestRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.List;

/**
 * This class provides the search suggestions from recent queries and static event tags.
 *
 * See http://developer.android.com/guide/topics/search/adding-recent-query-suggestions.html.
 */
public class EventSearchSuggestionsProvider extends SearchRecentSuggestionsProvider {
    private final static String AUTHORITY = "com.eventshigh.nearme.app.EventSearchSuggestions";
    private final static int MODE = DATABASE_MODE_QUERIES;
    private final static int SUGGESTION_ID_START = 10000;
    private final static int EVENT_ID_START = 20000;

    private SuggestEvent[] allEvents;
    private EventSuggestRequest requestEvents;

    private String[] allTags;
    private TagsSuggestRequest requestTags;

    public EventSearchSuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        loadTags();
        loadEvents();

        if (selectionArgs.length <=0 || selectionArgs[0].isEmpty()) {
            return super.query(uri, projection, selection, selectionArgs, sortOrder);
        }

        MatrixCursor suggestionsCursor = new MatrixCursor(new String[] {
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_QUERY,
                SearchManager.SUGGEST_COLUMN_ICON_1,
                SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA,
        });

        int idIndex = suggestionsCursor.getColumnIndex(BaseColumns._ID);
        int titleColumnIndex = suggestionsCursor.getColumnIndex(
                SearchManager.SUGGEST_COLUMN_TEXT_1);
        int queryColumnIndex = suggestionsCursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_QUERY);
        int iconColumnIndex = suggestionsCursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1);
        int intentActionColumnIndex = suggestionsCursor.getColumnIndex(
                SearchManager.SUGGEST_COLUMN_INTENT_ACTION);
        int intentDataColumnIndex = suggestionsCursor.getColumnIndex(
                SearchManager.SUGGEST_COLUMN_INTENT_DATA);
        int columnCount = suggestionsCursor.getColumnCount();

        String selectionArg = selectionArgs[0].toLowerCase();
        if (allTags != null) {
            for (int i = 0; i < allTags.length && suggestionsCursor.getCount() < 3; i++) {
                String tag = allTags[i];
                if (tag.contains(selectionArg)) {
                    Object[] newRow = new Object[columnCount];
                    newRow[idIndex] = SUGGESTION_ID_START + i;
                    newRow[queryColumnIndex] = tag;
                    newRow[iconColumnIndex] = R.drawable.ic_search_white_36dp;
                    newRow[titleColumnIndex] = Utils.capitalize(tag);
                    suggestionsCursor.addRow(newRow);
                }
            }
        }

        for (int i = 0; i < allEvents.length && suggestionsCursor.getCount() < 5; i++) {
            if (allEvents[i].name.contains(selectionArg)) {
                Object[] newRow = new Object[columnCount];
                newRow[idIndex] = EVENT_ID_START + i;
                newRow[queryColumnIndex] = allEvents[i].name;
                newRow[titleColumnIndex] = Utils.capitalize(allEvents[i].name);
                newRow[iconColumnIndex] = R.drawable.ic_event_white_24dp;
                newRow[intentActionColumnIndex] = Intent.ACTION_VIEW;
                newRow[intentDataColumnIndex] = EventsHighEndpoints.getEventDetailsURI(
                        City.getCity(allEvents[i].city), allEvents[i].id);
                suggestionsCursor.addRow(newRow);
            }
        }

        return suggestionsCursor;
    }

    public static void saveRecentQuery(Context context, String query) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, AUTHORITY, MODE);
        suggestions.saveRecentQuery(query, null);
    }

    public static void clearHistory(Context context) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, AUTHORITY, MODE);
        suggestions.clearHistory();
    }

    private synchronized void loadTags() {
        // If tags are already loaded or if request is in flight then return
        if (allTags != null || requestTags != null) {
            return;
        }

        requestTags = new TagsSuggestRequest(mTagSuggestListener, mTagErrorListener);
        VolleyHelper.addToRequestQueue(getContext(), requestTags);
    }

    private synchronized void loadEvents() {
        // Request is already in flight
        if (allEvents != null && requestEvents != null) {
            return;
        }

        City lastCity = new Account(getContext()).getLastCity();
        if (lastCity == null) {
            return;
        }

        requestEvents = new EventSuggestRequest(lastCity, mEventSuggestListener,
            mEventErrorListener);
        VolleyHelper.addToRequestQueue(getContext(), requestEvents);
    }

    private Listener<List<SuggestEvent>> mEventSuggestListener = new Listener<List<SuggestEvent>>() {
        @Override
        public void onResponse(List<SuggestEvent> suggestEvents, boolean isIntermediate) {
            synchronized (EventSearchSuggestionsProvider.this) {
                allEvents = suggestEvents.toArray(new SuggestEvent[suggestEvents.size()]);
                requestEvents = null;
            }
        }
    };

    private ErrorListener mEventErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            synchronized (EventSearchSuggestionsProvider.this) {
                // Try again
                requestEvents = null;
            }
        }
    };

    private Listener<List<String>> mTagSuggestListener = new Listener<List<String>>() {
        @Override
        public void onResponse(List<String> tagEvents, boolean isIntermediate) {
            synchronized (EventSearchSuggestionsProvider.this) {
                allTags = tagEvents.toArray(new String[tagEvents.size()]);
                requestTags = null;
            }
        }
    };

    private ErrorListener mTagErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            synchronized (EventSearchSuggestionsProvider.this) {
                // Try again
                requestTags = null;
            }
        }
    };
}
