package com.eventshigh.nearme.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;

/**
 * A dummy activity used to handle the settings actions.
 */
public class SettingsActivity extends BaseActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("com.eventshigh.delete_query_history")) {
                reportActionToAnalytics("deleteQueryHistory");

                EventSearchSuggestionsProvider.clearHistory(this);
                Snackbar.make(this.getViewForSnackbar(), R.string.message_delete_query_history,
                        Snackbar.LENGTH_SHORT).show();
            }
        }

        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("com.eventshigh.add_event")) {
                reportActionToAnalytics("addEvent");

                try {
                    startActivity(new Intent(Intent.ACTION_SENDTO,
                            Uri.parse("mailto:listings@eventshigh.com")));
                } catch (ActivityNotFoundException e) {
                    // No activity to open url. ignore.
                    Crashlytics.getInstance().core.logException(e);
                }
            }
        }

        finish();
    }

    @Override
    public View getViewForSnackbar() {
        return null;
    }
}
