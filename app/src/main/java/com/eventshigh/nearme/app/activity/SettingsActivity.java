package com.eventshigh.nearme.app.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;

import io.doorbell.android.Doorbell;

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
//                try {
//                    startActivity(new Intent(
//                            Intent.ACTION_SENDTO,
//                            Uri.parse("mailto:contact@eventshigh.com?subject=Mobile%20App%20Feedback")
//                    ));
//                } catch (ActivityNotFoundException e) {
//                    // Ignore.
//                }
                final AlertDialog dialog = new Doorbell(
                    this, 1119, "qXYpPYOzPQJ4CHtDFp2u0Kw2ZAXkPCPcd6cZ7LsLAj0NnJSqGKRI4Uy5Y7RoHmIK")
                    .setPoweredByVisibility(View.GONE)
                    .show();
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                });
            }

            if (intent.getAction().equals("com.eventshigh.delete_query_history")) {
                reportActionToAnalytics("deleteQueryHistory");

                EventSearchSuggestionsProvider.clearHistory(this);
                Toast.makeText(this, R.string.message_delete_query_history, Toast.LENGTH_SHORT).show();
            }
        }

        //finish();
    }
}
