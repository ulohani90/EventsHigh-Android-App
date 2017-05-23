package com.eventshigh.nearme.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

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
                showMessage(R.string.message_delete_query_history);
            }

            if (intent.getAction().equals("com.eventshigh.add_event")) {
                reportActionToAnalytics("addEvent");
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.eventshigh.pulse");
                if (launchIntent != null) {
                    startActivity(launchIntent);//null pointer check in case package name was not found
                }else{
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.eventshigh.pulse")));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.eventshigh.pulse")));
                    }

                }
                /*try {
                    startActivity(new Intent(Intent.ACTION_SENDTO,
                            Uri.parse("mailto:listings@eventshigh.com")));
                } catch (ActivityNotFoundException e) {
                    // No activity to open url. ignore.
                    Crashlytics.getInstance().core.logException(e);
                }*/
            }
            if(intent.getAction().equalsIgnoreCase("contact_support")){
                reportActionToAnalytics("contact_support");

                try {
                    startActivity(new Intent(Intent.ACTION_SENDTO,
                            Uri.parse("mailto:support@eventshigh.com")));
                } catch (ActivityNotFoundException e) {
                    // No activity to open url. ignore.
                    Crashlytics.getInstance().core.logException(e);
                }
            }


        }
        finish();
    }
}
