package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.provider.SearchRecentSuggestions;

/**
 * See http://developer.android.com/guide/topics/search/adding-recent-query-suggestions.html.
 */
public class EventSearchSuggestionsProvider extends SearchRecentSuggestionsProvider {
    private final static String AUTHORITY = "com.eventshigh.nearme.app.EventSearchRecentSuggestionsProvider";
    private final static int MODE = DATABASE_MODE_QUERIES;

    public EventSearchSuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    public static void saveRecentQuery(Context context, String query) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, AUTHORITY, MODE);
        suggestions.saveRecentQuery(query, null);
    }

    public static void clearHistory(Context context) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, AUTHORITY, MODE);
        suggestions.clearHistory();
    }
}
