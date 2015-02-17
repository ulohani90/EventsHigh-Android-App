package com.eventshigh.nearme.app.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.util.Log;

import com.eventshigh.nearme.app.utils.StreamUtils;

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

    private String[] allTags;

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
            if (allTags[i].startsWith(selectionArgs[0])) {
                Object[] newRow = new Object[columnCount];
                newRow[idIndex] = SUGGESTION_ID_START + i;
                newRow[queryColumnIndex] = allTags[i];
                newRow[titleColumnIndex] = allTags[i];
                suggestionsCursor.addRow(newRow);
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

    public static void saveRecentQuery(Context context, String query) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, AUTHORITY, MODE);
        suggestions.saveRecentQuery(query, null);
    }

    public static void clearHistory(Context context) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, AUTHORITY, MODE);
        suggestions.clearHistory();
    }
}
