package com.eventshigh.nearme.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;
import com.eventshigh.nearme.app.user.Account;

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
                showMessage(R.string.message_delete_query_history);
            }

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

            if (intent.getAction().equals("com.eventshigh.share_app")) {
                reportActionToAnalytics("shareApp", EventDetailActivity.PACKAGE_NAME_WHATSAPP);

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT,
                        String.format(getString(R.string.share_app_text), new Account(this).getAppDownloadLink()));
                shareIntent.setType("text/plain");
                shareIntent.setPackage(EventDetailActivity.PACKAGE_NAME_WHATSAPP);
                startActivity(shareIntent);
            }
        }
        finish();
    }
}
