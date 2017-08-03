package com.eventshigh.nearme.app.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.network.EventSuggestRequest;
import com.eventshigh.nearme.app.network.TagsSuggestRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.List;

/**
 * Created by umesh on 12/07/16.
 */
public class InterestSearchSuggestionsProvider extends SearchRecentSuggestionsProvider {
    private final static String AUTHORITY = "com.eventshigh.nearme.app.EventSearchSuggestions";
    private final static int MODE = DATABASE_MODE_QUERIES;
    private final static int SUGGESTION_ID_START = 10000;
    private final static int EVENT_ID_START = 20000;

    private EventSuggestRequest.SuggestEvent[] allEvents;
    private EventSuggestRequest requestEvents;

    private String[] allTags;
    private TagsSuggestRequest requestTags;

    public InterestSearchSuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        loadTags();

        if (selectionArgs.length <= 0 || selectionArgs[0].isEmpty()) {
            return super.query(uri, projection, selection, selectionArgs, sortOrder);
        }

        MatrixCursor suggestionsCursor = new MatrixCursor(new String[]{
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
                   // newRow[intentActionColumnIndex] = ;
                    suggestionsCursor.addRow(newRow);
                }
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

    private Response.Listener<List<EventSuggestRequest.SuggestEvent>> mEventSuggestListener = new Response.Listener<List<EventSuggestRequest.SuggestEvent>>() {
        @Override
        public void onResponse(List<EventSuggestRequest.SuggestEvent> suggestEvents, boolean isIntermediate) {
            synchronized (InterestSearchSuggestionsProvider.this) {
                allEvents = suggestEvents.toArray(new EventSuggestRequest.SuggestEvent[suggestEvents.size()]);
                requestEvents = null;
            }
        }
    };

    private Response.ErrorListener mEventErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            synchronized (InterestSearchSuggestionsProvider.this) {
                // Try again
                requestEvents = null;
            }
        }
    };

    private Response.Listener<List<String>> mTagSuggestListener = new Response.Listener<List<String>>() {
        @Override
        public void onResponse(List<String> tagEvents, boolean isIntermediate) {
            synchronized (InterestSearchSuggestionsProvider.this) {
                allTags = tagEvents.toArray(new String[tagEvents.size()]);
                requestTags = null;
            }
        }
    };

    private Response.ErrorListener mTagErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            synchronized (InterestSearchSuggestionsProvider.this) {
                // Try again
                requestTags = null;
            }
        }
    };
}