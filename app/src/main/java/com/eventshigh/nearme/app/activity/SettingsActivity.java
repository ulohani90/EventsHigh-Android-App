package com.eventshigh.nearme.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;


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

            if(intent.getAction().equalsIgnoreCase("contact_support")){
                reportActionToAnalytics("contact_support");

                try {
                    startActivity(new Intent(Intent.ACTION_SENDTO,
                            Uri.parse("mailto:support@eventshigh.com")));
                } catch (ActivityNotFoundException e) {
                    // No activity to open url. ignore.

                }
            }



        }
        finish();
    }
}
