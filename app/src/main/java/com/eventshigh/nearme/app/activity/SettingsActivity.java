package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.apptentive.android.sdk.Apptentive;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;
import com.eventshigh.nearme.app.utils.GAHelper;

/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 * <p/>
 * See http://developer.android.com/guide/topics/ui/settings.html.
 */
public class SettingsActivity extends Activity {
    private static final String LOG_TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onStart() {
        super.onStart();
        Apptentive.onStart(this);
    }

    @Override
    protected void onStop() {
        Apptentive.onStop(this);
        super.onStop();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GAHelper gaHelper = GAHelper.getInstance(this);
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("com.eventshigh.send_feedback")) {
                gaHelper.reportActionToAnalytics(LOG_TAG, "sendFeedback");

                try {
                    startActivity(new Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("mailto:contact@eventshigh.com?subject=Mobile%20App%20Feedback")
                    ));
//                    Apptentive.showMessageCenter(this);
                } catch (ActivityNotFoundException e) {
                    // Ignore.
                }
            }

            if (intent.getAction().equals("com.eventshigh.delete_query_history")) {
                gaHelper.reportActionToAnalytics(LOG_TAG, "deleteQueryHistory");

                EventSearchSuggestionsProvider.clearHistory(this);
                Toast.makeText(this, R.string.message_delete_query_history, Toast.LENGTH_SHORT).show();
            }
        }

        finish();
    }
}
