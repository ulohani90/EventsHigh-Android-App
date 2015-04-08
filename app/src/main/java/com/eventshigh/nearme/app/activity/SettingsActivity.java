package com.eventshigh.nearme.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

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
            if (intent.getAction().equals("com.eventshigh.send_feedback")) {
                reportActionToAnalytics("sendFeedback");
                try {
                    startActivity(new Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("mailto:contact@eventshigh.com?subject=Mobile%20App%20Feedback")
                    ));
                } catch (ActivityNotFoundException e) {
                    // Ignore.
                }
            }

            if (intent.getAction().equals("com.eventshigh.delete_query_history")) {
                reportActionToAnalytics("deleteQueryHistory");

                EventSearchSuggestionsProvider.clearHistory(this);
                Toast.makeText(this, R.string.message_delete_query_history, Toast.LENGTH_SHORT).show();
            }
        }

        finish();
    }
}
