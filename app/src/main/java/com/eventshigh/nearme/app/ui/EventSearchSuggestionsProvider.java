package com.eventshigh.nearme.app.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.StreamUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

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

    private String[] allTags;
    private SuggestEvent[] allEvents;
    private JsonArrayRequest request;

    public EventSearchSuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Cursor recentsCursor = super.query(uri, projection, selection, selectionArgs, sortOrder);

        if (selectionArgs.length <=0 || selectionArgs[0].isEmpty()) {
            return recentsCursor;
        }

        readTags();
        MatrixCursor suggestionsCursor = new MatrixCursor(recentsCursor.getColumnNames());
        int idIndex = recentsCursor.getColumnIndex(BaseColumns._ID);
        int titleColumnIndex = recentsCursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1);
        int queryColumnIndex = recentsCursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_QUERY);
        int columnCount = recentsCursor.getColumnCount();
        for (int i = 0; i < allTags.length && suggestionsCursor.getCount() < 5; i++) {
            String tag = allTags[i];
            if (tag.startsWith(selectionArgs[0])) {
                Object[] newRow = new Object[columnCount];
                newRow[idIndex] = SUGGESTION_ID_START + i;
                newRow[queryColumnIndex] = tag;
                newRow[titleColumnIndex] = tag;
                suggestionsCursor.addRow(newRow);
            }
        }

        if (allEvents == null) {
            loadEvents();
        } else {
            int iconColumnIndex = suggestionsCursor.getColumnIndex(
                    SearchManager.SUGGEST_COLUMN_ICON_1);
            for (int i = 0; i < allEvents.length && suggestionsCursor.getCount() < 5; i++) {
                String suggestion = allEvents[i].name;
                if (suggestion.contains(selectionArgs[0])) {
                    Object[] newRow = new Object[columnCount];
                    newRow[idIndex] = EVENT_ID_START + i;
                    newRow[queryColumnIndex] = EventsHighEndpoints.getEventDetailsURI(
                            City.getCity(allEvents[i].city), allEvents[i].id);
                    newRow[titleColumnIndex] = suggestion;
                    newRow[iconColumnIndex] = R.drawable.ic_location_on_white_36dp;
                    suggestionsCursor.addRow(newRow);
                }
            }
        }

        return new MergeCursor(new Cursor[]{recentsCursor, suggestionsCursor});
    }

    private synchronized void readTags() {
        if (allTags == null) {
            try {
                allTags = StreamUtils.readAssetFile(getContext(), "all_tags.txt");
            } catch (IOException e) {
                Log.w(EventSearchSuggestionsProvider.class.getSimpleName(), "Failed to read tags", e);
                allTags = new String[]{};
            }
        }
    }

    private synchronized void loadEvents() {
        // Request is already in flight
        if (request != null) {
            return;
        }

        request = new JsonArrayRequest("https://s3-ap-southeast-1.amazonaws.com/"
                + "ehautocomplete/autocomplete_events_bangalore.json",
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray, boolean b) {
                        allEvents = new SuggestEvent[jsonArray.length()];
                        for (int i = 0; i < jsonArray.length(); i++) {
                            try {
                                allEvents[i] = SuggestEvent.parse(jsonArray.getJSONObject(i));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                synchronized (this) {
                    // Try again
                    request = null;
                }
            }
        });

        VolleyHelper.addToRequestQueue(getContext(), request);
    }

    public static void saveRecentQuery(Context context, String query) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, AUTHORITY, MODE);
        suggestions.saveRecentQuery(query, null);
    }

    public static void clearHistory(Context context) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, AUTHORITY, MODE);
        suggestions.clearHistory();
    }

    private static class SuggestEvent {
        private final String id;
        private final String city;
        private final String type;
        private final String name;

        public SuggestEvent(String id, String city, String type, String name) {
            this.id = id;
            this.city = city;
            this.type = type;
            this.name = name;
        }

        public static SuggestEvent parse(JSONObject jsonObject) throws JSONException {
            return new SuggestEvent(jsonObject.getString("id"), jsonObject.getString("city"),
                    jsonObject.getString("type"), jsonObject.getString("name"));
        }
    }
}
