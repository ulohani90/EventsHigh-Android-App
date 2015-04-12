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

//                final AlertDialog dialog = new Doorbell(
//                    this, 1119, "qXYpPYOzPQJ4CHtDFp2u0Kw2ZAXkPCPcd6cZ7LsLAj0NnJSqGKRI4Uy5Y7RoHmIK")
//                    .setPoweredByVisibility(View.GONE)
//                    .show();
//                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                    @Override
//                    public void onDismiss(DialogInterface dialog) {
//                        finish();
//                    }
//                });
//                return;

//                // Initialize Zendesk
//                ZendeskConfig.INSTANCE.init(this,
//                    "https://eventshigh.zendesk.com",
//                    "e768bcce15686cef22667ce751438e5637c2f22957521a51",
//                    "mobile_sdk_client_c134c9f3e705c0b37a78");
//
//                // Anonymous reporting
//                Identity anonymousIdentity = new AnonymousIdentity.Builder().build();
//                ZendeskConfig.INSTANCE.setIdentity(anonymousIdentity);
//
//                // Set the configuration used by the Contact Zendesk component
//                ZendeskConfig.INSTANCE.setContactConfiguration(new FeedbackConfiguration());
//                Intent feedbackIntent = new Intent(this, ContactZendeskActivity.class);
//                startActivity(feedbackIntent);

//                Helpshift.install(getApplication(),
//                    "436ada7afb6715ef004759aea5bfd506", // API Key
//                    "eventshigh.helpshift.com", // Domain Name
//                    "eventshigh_platform_20150412145115945-7dc2b704461dbe7"); // App ID
//                Helpshift.showConversation(this);
            }
            if (intent.getAction().equals("com.eventshigh.delete_query_history")) {
                reportActionToAnalytics("deleteQueryHistory");

                EventSearchSuggestionsProvider.clearHistory(this);
                Toast.makeText(this, R.string.message_delete_query_history, Toast.LENGTH_SHORT).show();
            }
        }

        finish();
    }

//    private class FeedbackConfiguration extends BaseZendeskFeedbackConfiguration {
//
//        @Override
//        public String getRequestSubject() {
//            return "Feedback";
//        }
//    }
}
